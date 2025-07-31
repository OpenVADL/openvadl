use std::{cmp::min, fmt::Debug, time::Duration};

use color_eyre::eyre::Result;
use cosim_lib::ipc::cstructs::{MAX_REGISTER_DATA_SIZE, SHMRegister, SHMSTRING_MAX_LEN, SHMString};
use ratatui::{
    Frame,
    crossterm::event::{self, Event, KeyCode, KeyEventKind},
    layout::{Constraint, Layout, Rect},
    text::{Line, Text},
    widgets::Block,
};

pub type Register = cosim_lib::ipc::cstructs::SHMRegister;
pub type Registers = Vec<cosim_lib::ipc::cstructs::SHMRegister>;

fn shmstr(s: &str) -> SHMString {
    let len = s.len();
    assert!(len < SHMSTRING_MAX_LEN);
    let mut slice = [0u8; SHMSTRING_MAX_LEN];
    slice[..len].copy_from_slice(s.as_bytes());
    SHMString::new(len, slice)
}

// TODO: for testing purposes, remove later
fn reg(name: &str, data: &[u8]) -> Register {
    let size = data.len();
    assert!(size <= MAX_REGISTER_DATA_SIZE);
    let mut slice = [0u8; MAX_REGISTER_DATA_SIZE];
    slice[..size].copy_from_slice(data);

    SHMRegister::new(size as i32, slice, shmstr(name))
}

#[derive(Default, Debug)]
struct Model {
    state_pos: usize,
    view_size: usize,
    is_running: bool,
    states: Vec<Registers>,
}

impl Model {
    fn incr_state_pos(&mut self) -> bool {
        // TODO: fetch states count
        if self.state_pos < self.states.len() - 1 {
            self.state_pos += 1;
            true
        } else {
            false
        }
    }

    fn decr_state_pos(&mut self) -> bool {
        if self.state_pos > 0 {
            self.state_pos -= 1;
            true
        } else {
            false
        }
    }

    fn load_state_at_pos(&mut self) {
        // TODO
    }

    fn get_state(&self) -> &Registers {
        &self.states[self.state_pos]
    }
}

type Message = event::Event;

fn update(model: &mut Model, msg: Message) -> Option<Message> {
    match msg {
        Event::Key(key_event) if key_event.kind == KeyEventKind::Press => {
            handle_key(model, key_event)
        }
        _ => None,
    }
}

fn view(model: &mut Model, frame: &mut Frame) {
    let l = Layout::horizontal([Constraint::Percentage(100), Constraint::Min(40)]);
    let [main, side] = l.areas(frame.area());

    let from = usize::saturating_sub(model.state_pos, model.view_size);
    let to = usize::saturating_add(model.state_pos, model.view_size);
    let to = min(to, model.states.len() - 1);
    let elems = &model.states[from..=to];

    let mut elem_constraints = vec![];
    for _ in 0..elems.len() - 1 {
        elem_constraints.push(Constraint::Fill(10));
        elem_constraints.push(Constraint::Fill(1));
    }

    let elem_layout = Layout::vertical(elem_constraints);
    let elem_rects = elem_layout.split(main);

    for i in (0..elem_rects.len()).step_by(2) {
        let s = &elems[i];
        let rect = elem_rects[i];
        let tran = elem_rects[i + 1];
        let trans_text = format!("#step: {}", i + 1);
        view_registers_box(s, frame, rect);
        view_transition(&trans_text, frame, tran);
    }

    view_side_bar(frame, side);
}

fn fetch_event() -> Result<Option<Message>> {
    if event::poll(Duration::from_millis(250))? {
        let event = event::read()?;
        Ok(Some(event))
    } else {
        Ok(None)
    }
}

fn handle_key(model: &mut Model, key: event::KeyEvent) -> Option<Message> {
    match key.code {
        KeyCode::Esc | KeyCode::Char('q') => {
            model.is_running = false;
            None
        }
        KeyCode::Down => {
            if model.incr_state_pos() {
                model.load_state_at_pos();
            }
            None
        }
        KeyCode::Up => {
            if model.decr_state_pos() {
                model.load_state_at_pos();
            }
            None
        }
        KeyCode::Char('+') => {
            if model.view_size < model.states.len() - 1 {
                model.view_size += 1;
            }
            None
        }
        KeyCode::Char('-') => {
            model.view_size = usize::saturating_sub(model.view_size, 1);
            None
        }
        _ => None,
    }
}

fn generate_state() -> Registers {
    vec![
        reg("x10", &[0, 0, 0, 1]),
        reg("x11", &[0, 0, 0, 2]),
        reg("x12", &[0, 0, 0, 3]),
        reg("x13", &[0, 0, 0, 4]),
        reg("x14", &[0, 0, 0, 5]),
    ]
}

fn main() -> Result<()> {
    color_eyre::install()?;
    let mut terminal = ratatui::init();
    let mut model = Model {
        is_running: true,
        states: vec![
            generate_state(),
            generate_state(),
            generate_state(),
            generate_state(),
            generate_state(),
        ],
        state_pos: 0,
        view_size: 1,
    };

    while model.is_running {
        terminal.draw(|f| view(&mut model, f))?;
        let mut next_msg = fetch_event()?;

        while let Some(msg) = next_msg {
            next_msg = update(&mut model, msg);
        }
    }

    ratatui::restore();
    Ok(())
}

fn view_side_bar(frame: &mut Frame, area: Rect) {
    frame.render_widget(Block::bordered().title("filter"), area);
}

fn view_debug(model: &Model, frame: &mut Frame, area: Rect) {
    let debug_text = Text::from(format!("{model:#?}"));
    frame.render_widget(debug_text, area);
}

fn view_transition(s: &str, frame: &mut Frame, area: Rect) {
    let t = format!("↓ {s}");
    frame.render_widget(t, area);
}

fn view_registers_box(s: &Vec<SHMRegister>, frame: &mut Frame, rect: Rect) {
    let res = s
        .iter()
        .map(|reg| {
            let name = reg.name.as_str().to_string();
            let value = reg.data_slice_fmt();
            Line::from(format!("{name}: {value}"))
        })
        .collect::<Vec<Line>>();

    frame.render_widget(Text::from(res), rect);
}
