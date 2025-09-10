use std::{collections::HashMap, fmt::Debug};

use clap::Parser;

use cosim_lib::{
    config::Config,
    db::{
        dbstructs::{BrokerData, Client, ClientEntry, Register, TBInsnInfo},
        select_cosim_run_clients, select_cosim_run_entries_at_run_count,
        select_cosim_run_entries_length,
    },
    trace::connect,
};
use cursive::{
    Cursive,
    event::EventResult,
    theme::{BaseColor, ColorStyle, Style},
    utils::markup::StyledString,
    view::{Resizable, Scrollable},
    views::{Canvas, Dialog, EditView, HideableView, LinearLayout, Panel, TextView},
};
use cursive::{
    traits::*,
    views::{Checkbox, ListView},
};
use figment::{
    Figment,
    providers::{Format, Toml},
};
use rusqlite::Connection;

const MIN_STATE_POS: usize = 1;

const THEME: &str = "
shadow = false
borders = \"simple\"

[colors]
    background = \"black\"
    shadow     = [\"#000000\", \"black\"]
    view       = \"black\"

    primary   = [\"white\"]
    secondary = \"#EEEEEE\"
    tertiary  = \"#444444\"

    title_primary   = \"#ff5555\"
    title_secondary = \"#ffff55\"

    highlight          = \"#F00\"
    highlight_inactive = \"#5555FF\"
";

type ClientEntryRow = Vec<ClientEntry>;
type RegisterFilter = HashMap<String, bool>;

#[derive(Debug, Clone)]
struct Model {
    run_id: i64,
    state_pos: usize,
    clients: Vec<Client>,
    entry_rows: Vec<ClientEntryRow>,
    register_filter: RegisterFilter,
}

#[derive(Debug)]
struct AppState {
    model: Model,
    db_connection: Connection,
    config: Config,
}

impl Model {
    fn client_ids(&self) -> Vec<i32> {
        self.clients.iter().map(|c| c.id).collect()
    }

    fn load_next_state(&mut self, connection: &mut Connection) -> bool {
        self.load_state_at_pos(connection, self.state_pos + 1)
    }

    fn load_prev_state(&mut self, connection: &mut Connection) -> bool {
        self.load_state_at_pos(connection, self.state_pos.saturating_sub(1))
    }

    fn load_current_state(&mut self, connection: &mut Connection) -> bool {
        self.load_state_at_pos(connection, self.state_pos)
    }

    fn load_state_at_pos(&mut self, connection: &mut Connection, new_pos: usize) -> bool {
        if new_pos < MIN_STATE_POS {
            return false;
        }

        let max_state_pos = select_cosim_run_entries_length(connection, self.run_id)
            .expect("load_state_at_pos failed");

        if new_pos > max_state_pos as usize {
            return false;
        }

        let client_ids = self.client_ids();

        let mut rows = vec![];
        if new_pos > MIN_STATE_POS {
            let prev_row = select_cosim_run_entries_at_run_count(
                connection,
                &client_ids,
                (new_pos - 1) as u64,
            )
            .expect("load_state_at_pos failed");
            rows.push(prev_row);
        }

        let this_row =
            select_cosim_run_entries_at_run_count(connection, &client_ids, new_pos as u64)
                .expect("load_state_at_pos failed");

        rows.push(this_row);

        if new_pos < max_state_pos as usize {
            let next_row = select_cosim_run_entries_at_run_count(
                connection,
                &client_ids,
                (new_pos + 1) as u64,
            )
            .expect("load_state_at_pos failed");

            rows.push(next_row);
        }

        self.entry_rows = rows;
        self.state_pos = new_pos;
        true
    }
}

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
pub struct Cli {
    /// Path to the (toml) config file
    #[arg(short, long, value_name="FILE", default_value_t = default_config_file())]
    pub config: String,

    /// The cosimulation-run to load into the tui
    #[arg(long, value_name = "RUN-ID")]
    pub run_id: i64,
}

fn default_config_file() -> String {
    "./config.toml".into()
}

fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();

    let mut config: Config = Figment::new().merge(Toml::file(cli.config)).extract()?;

    config.qemu.set_inverse_reg_map();

    let run_id = cli.run_id;

    let mut db_connection = connect(&config)?;

    let clients = select_cosim_run_clients(&mut db_connection, run_id)?;
    assert!(!clients.is_empty(), "should've at least one client");

    let mut model = Model {
        run_id,
        state_pos: 414,
        clients,
        entry_rows: vec![],
        register_filter: init_register_filter(&config),
    };

    model.load_current_state(&mut db_connection);

    let app_state = AppState {
        model,
        db_connection,
        config,
    };

    let mut siv = cursive::default();

    siv.set_user_data(app_state);

    siv.load_toml(THEME).unwrap();

    siv.add_global_callback('q', |s| s.quit());
    siv.add_global_callback('j', |s| {
        let app_state = get_app_state(s);
        let model = &mut app_state.model;
        if model.load_next_state(&mut app_state.db_connection) {
            refresh_clients_view(s);
        }
    });

    siv.add_global_callback('k', |s| {
        let app_state = get_app_state(s);
        let model = &mut app_state.model;
        if model.load_prev_state(&mut app_state.db_connection) {
            refresh_clients_view(s);
        }
    });

    siv.add_global_callback('g', |s| {
        s.add_layer(view_enter_run_count_dialog());
    });

    siv.add_global_callback('t', |s| {
        let mut events = vec![];
        s.call_on_all_named("filter-checkbox", |checkbox: &mut Checkbox| {
            events.push(checkbox.check())
        });
        for event in events {
            if let EventResult::Consumed(Some(callback)) = event {
                callback(s)
            }
        }
    });

    siv.add_global_callback('n', |s| {
        let mut events = vec![];
        s.call_on_all_named("filter-checkbox", |checkbox: &mut Checkbox| {
            events.push(checkbox.uncheck())
        });
        for event in events {
            if let EventResult::Consumed(Some(callback)) = event {
                callback(s);
            }
        }
    });

    let app_state = get_app_state(&mut siv);
    let model = &app_state.model;
    let config = &app_state.config;

    let clients_view = view_clients(model, config);
    let main_panel = Panel::new(clients_view)
        .title("Main")
        .with_name("main-panel");

    let filter_panel = Panel::new(view_filter(model, config)).title("Filter");

    let mut main_layout = LinearLayout::horizontal();
    main_layout.add_child(main_panel);
    main_layout.add_child(filter_panel);

    siv.add_fullscreen_layer(main_layout.full_screen());

    siv.run();

    Ok(())
}

fn refresh_clients_view(s: &mut Cursive) {
    let app_state = get_app_state(s);
    let model = &app_state.model;

    // NOTE: Maybe a RwLock could be a better solution here
    let model = model.clone();
    let config = &app_state.config.clone();

    if let Some(mut panel) = s.find_name::<Panel<LinearLayout>>("main-panel") {
        *panel.get_inner_mut() = view_clients(&model, config)
    }
}

fn view_enter_run_count_dialog() -> Dialog {
    let input = EditView::new()
        .on_submit(|s, input| {
            let run_count = input.parse::<usize>();
            match run_count {
                Ok(run_count) => {
                    let app_state = get_app_state(s);
                    let successfully_loaded = app_state
                        .model
                        .load_state_at_pos(&mut app_state.db_connection, run_count);

                    if successfully_loaded {
                        refresh_clients_view(s);
                        s.pop_layer();
                    } else {
                        let max_run_count = select_cosim_run_entries_length(
                            &mut app_state.db_connection,
                            app_state.model.run_id,
                        )
                        .expect("select_cosim_run_entries_length failed");

                        s.add_layer(Dialog::info(format!(
                            "Please select a run-count between (including) 1 and {max_run_count}"
                        )));
                    }
                }
                Err(_) => {
                    s.add_layer(Dialog::info("Please enter a valid number"));
                }
            }
        })
        .fixed_width(20);

    Dialog::around(input)
        .title("Jump to run-count")
        .button("Cancel", |s| {
            s.pop_layer();
        })
}

fn get_app_state(s: &mut Cursive) -> &mut AppState {
    s.user_data::<AppState>().unwrap()
}

fn view_filter(model: &Model, config: &Config) -> impl View {
    let registers = model.entry_rows[0]
        .iter()
        .map(|e| &e.broker.data()[0].registers)
        .max_by_key(|regs| regs.len())
        .expect("view_filter: registers");

    let names = registers.iter().map(|reg| reg.mapped_name(config));

    let mut list = ListView::new();

    for name in names {
        let default_is_checked = config.qemu.gdb_reg_map_inverse.contains_key(name)
            && !config.qemu.ignore_registers.contains(name);
        let name = name.to_owned();
        let name_copy = name.clone();
        let checkbox = Checkbox::new()
            .with_checked(default_is_checked)
            .on_change(move |s, checked| {
                let name_copy = name_copy.clone();

                let app_state = get_app_state(s);
                app_state
                    .model
                    .register_filter
                    .insert(name_copy.clone(), checked);

                // TODO: maybe make NamedView<HideableView<TextView>>> a component
                s.call_on_all_named(&name_copy, |v: &mut HideableView<TextView>| {
                    v.set_visible(checked);
                });
            })
            .with_name("filter-checkbox");

        list.add_child(name, checkbox);
    }

    list.scrollable()
}

fn init_register_filter(config: &Config) -> RegisterFilter {
    let mut map = HashMap::new();
    for name in config.qemu.gdb_reg_map_inverse.keys() {
        map.insert(name.to_owned(), true);
    }

    for ignore_name in &config.qemu.ignore_registers {
        map.insert(ignore_name.to_owned(), false);
    }

    map
}

fn view_clients(model: &Model, config: &Config) -> LinearLayout {
    let mut layout = LinearLayout::horizontal();

    let mut client_layouts = model.entry_rows[0]
        .iter()
        .map(|entry| {
            let title = entry
                .client
                .name
                .clone()
                .unwrap_or(entry.client.id.to_string());
            Panel::new(LinearLayout::vertical()).title(title)
        })
        .collect::<Vec<_>>();

    for entry_row in &model.entry_rows {
        for (idx, entry) in entry_row.iter().enumerate() {
            let client_layout = client_layouts[idx].get_inner_mut();
            let entry = view_client_entry(entry, &model.register_filter, config);
            client_layout.add_child(entry);
        }
    }

    for client_layout in client_layouts {
        layout.add_child(client_layout);
    }

    layout
}

fn view_client_entry(
    entry: &ClientEntry,
    register_filter: &RegisterFilter,
    config: &Config,
) -> impl View {
    let insns = view_instructions(&entry.broker);
    let registers = view_broker_data(&entry.broker, register_filter, config);

    let canvas = Canvas::new(()).with_draw(|_, p| p.print_hline((0, 0), p.size.x, "─"));

    let entry_layout = LinearLayout::vertical()
        .child(insns)
        .child(canvas)
        .child(registers);

    let start_pc = format_pc(get_start_pc(&entry.broker));
    let run_count = &entry.run_count;

    let entry_title = format!("start-pc = {start_pc}, run-count = {run_count}");
    Panel::new(entry_layout).title(entry_title)
}

fn view_broker_data(
    data: &BrokerData,
    register_filter: &RegisterFilter,
    config: &Config,
) -> impl View {
    let registers = match data {
        BrokerData::TB(data) => &data.cpus[0].registers,
        BrokerData::Insn(data) => &data.cpus[0].registers,
    };
    view_registers(registers, register_filter, config)
}

fn view_registers(
    regs: &[Register],
    register_filter: &RegisterFilter,
    config: &Config,
) -> impl View {
    let mut layout = LinearLayout::vertical();
    for reg in regs {
        layout.add_child(view_register(reg, register_filter, config));
    }

    layout.scrollable().max_height(25).full_width()
}

fn view_register(reg: &Register, register_filter: &RegisterFilter, config: &Config) -> impl View {
    let name = reg.mapped_name(config);
    let mut span = StyledString::new();
    span.append_plain(name);
    span.append_plain(": ");
    span.append_styled(
        reg.data_fmt(),
        Style {
            effects: Default::default(),
            color: ColorStyle::new(BaseColor::Red, BaseColor::Black),
        },
    );

    let is_visible = register_filter.get(name);

    HideableView::new(TextView::new(span))
        .visible(is_visible.is_some_and(|vis| *vis))
        .with_name(name)
}

// fn view_execution_step(data: &ClientEntry) -> impl View {
// }

fn view_instructions(data: &BrokerData) -> impl View {
    match data {
        BrokerData::TB(data) => {
            let tb = &data.tb_info;

            let mut insns = LinearLayout::vertical();
            for insn in &tb.insns_info {
                let insn = view_instruction(insn);
                insns.add_child(insn);
            }

            insns
        }
        BrokerData::Insn(data) => {
            let insn = view_instruction(&data.insn_info);

            LinearLayout::vertical().child(insn)
        }
    }
}

fn view_instruction(insn: &TBInsnInfo) -> impl View {
    let pc = format!("{:02X?}", insn.pc);

    let disas = if insn.disas.is_empty() {
        "N/A"
    } else {
        &insn.disas
    };

    let content = format!("{pc}: {disas} ({})", insn.data_fmt());
    TextView::new(content)
}

fn get_start_pc(data: &BrokerData) -> u64 {
    match data {
        BrokerData::TB(data) => data.tb_info.pc,
        BrokerData::Insn(data) => data.insn_info.pc,
    }
}

fn format_pc(pc: u64) -> String {
    format!("{pc:02X?}")
}
