// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lsp.integration;

import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DefinitionCapabilities;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import vadl.lsp.VadlLanguageServer;

/**
 * The base for all lsp integration tests. Provides a fresh, fully plumbed instance of the
 * VadlLanguageServer for subclasses to run tests against.
 */
public abstract class IntegrationTest {
  protected VadlLanguageServer server;
  protected TextDocumentService textService;
  protected MockLanguageClient client;

  @BeforeEach
  public void createServer() throws ExecutionException, InterruptedException {
    server = new VadlLanguageServer(new VadlLanguageServer.Settings(false));
    client = new MockLanguageClient();
    server.connect(client);
    server.setListeningFuture(Futures.immediateFuture(null));

    // Client Capabilities - these may need to be adjusted if new features are added to the server
    var textDocumentCapabilities = new TextDocumentClientCapabilities();
    textDocumentCapabilities.setDefinition(new DefinitionCapabilities());
    textDocumentCapabilities.setPublishDiagnostics(new PublishDiagnosticsCapabilities());

    var params = new InitializeParams();
    params.setCapabilities(new ClientCapabilities(null,
        textDocumentCapabilities, null));
    server.initialize(params).get();
    server.initialized(new InitializedParams());

    textService = server.getTextDocumentService();
  }

  @AfterEach
  public void stopServer() throws ExecutionException, InterruptedException {
    server.shutdown().get();
    server.exit();
    server.tearDown();
  }

  protected void openDocument(String uri, String text) {
    textService.didOpen(new DidOpenTextDocumentParams(new TextDocumentItem(
      uri, "vadl", 0, text
    )));
  }


  public static class MockLanguageClient implements LanguageClient {
    private final List<PublishDiagnosticsParams> publishedDiagnostics = new ArrayList<>();

    /**
     * Executes the provided runnable, then waits for the server to publish diagnostics once. This
     * method either returns when the server published one set of diagnostic results, or the
     * wait time expired.
     *
     * <p>Use this method to have a tight coupling between triggering diagnostics and collecting
     * them (even though this is a concurrent system).
     *
     * @param runnable the action that should cause the server to publish diagnostics
     * @return all diagnostic results received so far
     */
    protected List<PublishDiagnosticsParams> doThenWaitForPublishDiagnostics(Runnable runnable) {
      synchronized (publishedDiagnostics) {
        runnable.run();

        try {
          publishedDiagnostics.wait(350);
        } catch (InterruptedException e) {
          // Nothing
        }

        return new ArrayList<>(publishedDiagnostics);
      }
    }

    @Override
    public void telemetryEvent(Object object) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
      synchronized (publishedDiagnostics) {
        publishedDiagnostics.add(diagnostics);
        publishedDiagnostics.notifyAll();
      }
    }

    @Override
    public void showMessage(MessageParams messageParams) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(
        ShowMessageRequestParams requestParams) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void logMessage(MessageParams message) {
      throw new UnsupportedOperationException();
    }
  }
}
