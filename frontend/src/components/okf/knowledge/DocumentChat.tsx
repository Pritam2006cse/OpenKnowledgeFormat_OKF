
import { useEffect, useState, type FormEvent } from "react";
import { MessageCircle, Send, LoaderCircle } from "lucide-react";
import {
  chatWithDocument,
  type ChatMessage,
} from "@/lib/okf/api";

interface DocumentChatProps {
  markdownName: string;
}

export function DocumentChat({
  markdownName,
}: DocumentChatProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // A different document starts a fresh conversation.
  useEffect(() => {
    setMessages([]);
    setInput("");
    setError("");
    setIsOpen(false);
  }, [markdownName]);

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    const question = input.trim();

    if (!question || loading) return;

    const previousHistory = messages;

    const userMessage: ChatMessage = {
      role: "user",
      content: question,
    };

    setMessages([...previousHistory, userMessage]);
    setInput("");
    setError("");
    setLoading(true);

    try {
      const response = await chatWithDocument(
        markdownName,
        question,
        previousHistory,
      );

      setMessages((current) => [
        ...current,
        {
          role: "assistant",
          content: response.answer,
        },
      ]);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Could not get an answer.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <section className="mt-6 rounded-2xl border bg-card p-4 shadow-soft">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-xl bg-accent text-accent-foreground">
            <MessageCircle className="h-5 w-5" />
          </div>

          <div>
            <h3 className="font-semibold">
              Chat with this document
            </h3>
            <p className="text-xs text-muted-foreground">
              Ask questions about {markdownName}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setIsOpen((value) => !value)}
          className="rounded-xl border px-3 py-2 text-sm font-medium hover:bg-muted"
        >
          {isOpen ? "Hide chat" : "Open chat"}
        </button>
      </div>

      {isOpen && (
        <div className="mt-4 space-y-4">
          <div
            className="max-h-80 min-h-24 space-y-3 overflow-y-auto rounded-xl border bg-background p-3"
            aria-live="polite"
          >
            {messages.length === 0 && (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Ask your first question about this document.
              </p>
            )}

            {messages.map((message, index) => (
              <div
                key={`${index}-${message.role}`}
                className={`flex ${
                  message.role === "user"
                    ? "justify-end"
                    : "justify-start"
                }`}
              >
                <div
                  className={`max-w-[90%] whitespace-pre-wrap break-words rounded-2xl px-3 py-2.5 text-sm ${
                    message.role === "user"
                      ? "bg-primary text-primary-foreground"
                      : "border bg-muted"
                  }`}
                >
                  <p className="mb-1 text-xs font-semibold opacity-75">
                    {message.role === "user"
                      ? "You"
                      : "OKF Assistant"}
                  </p>
                  {message.content}
                </div>
              </div>
            ))}

            {loading && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <LoaderCircle className="h-4 w-4 animate-spin" />
                Generating answer...
              </div>
            )}
          </div>

          {error && (
            <p
              role="alert"
              className="rounded-xl border border-destructive/30 p-3 text-sm text-destructive"
            >
              {error}
            </p>
          )}

          <form
            onSubmit={handleSubmit}
            className="flex items-end gap-2"
          >
            <textarea
              value={input}
              onChange={(event) =>
                setInput(event.target.value)
              }
              onKeyDown={(event) => {
                if (
                  event.key === "Enter" &&
                  !event.shiftKey &&
                  !event.nativeEvent.isComposing
                ) {
                  event.preventDefault();
                  event.currentTarget.form?.requestSubmit();
                }
              }}
              placeholder="Ask a question about this document..."
              rows={2}
              disabled={loading}
              className="min-w-0 flex-1 resize-y rounded-xl border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring disabled:opacity-60"
            />

            <button
              type="submit"
              disabled={loading || !input.trim()}
              aria-label="Send message"
              className="inline-flex h-10 items-center gap-2 rounded-xl bg-primary px-3 text-sm font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? (
                <LoaderCircle className="h-4 w-4 animate-spin" />
              ) : (
                <Send className="h-4 w-4" />
              )}
              <span className="hidden sm:inline">
                Send
              </span>
            </button>
          </form>

          <p className="text-xs text-muted-foreground">
            Answers are generated locally using Ollama and the
            selected Markdown document.
          </p>
        </div>
      )}
    </section>
  );
}
