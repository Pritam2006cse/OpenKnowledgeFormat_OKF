import { useEffect, useState, type FormEvent } from "react";
import {
  MessageCircle,
  Send,
  LoaderCircle,
  FileText,
  Check,
  X,
} from "lucide-react";

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

  const [messages, setMessages] =
    useState<ChatMessage[]>([]);

  const [input, setInput] = useState("");

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  /*
   * Stores a document for which the user has
   * explicitly given permission.
   */
  const [approvedDocument, setApprovedDocument] =
    useState<string | undefined>(undefined);

  /*
   * Stores a pending permission request.
   *
   * Example:
   *
   * User is chatting with:
   * Computer_Networks.md
   *
   * User asks:
   * "What is lithium ion battery?"
   *
   * Backend returns:
   *
   * CROSS_DOCUMENT_PERMISSION
   * suggestedDocument = Battery_Technology.md
   */
  const [permissionDocument, setPermissionDocument] =
    useState<string | undefined>(undefined);

  /*
   * A different document starts a fresh conversation.
   */
  useEffect(() => {
    setMessages([]);
    setInput("");
    setError("");
    setIsOpen(false);

    setApprovedDocument(undefined);
    setPermissionDocument(undefined);
  }, [markdownName]);

  async function handleSubmit(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();

    const question = input.trim();

    if (!question || loading) {
      return;
    }

    const previousHistory = messages;

    const userMessage: ChatMessage = {
      role: "user",
      content: question,
    };

    setMessages([
      ...previousHistory,
      userMessage,
    ]);

    setInput("");
    setError("");
    setLoading(true);

    /*
     * A new question starts without a pending
     * permission request.
     */
    setPermissionDocument(undefined);

    try {
      const response =
        await chatWithDocument(
          markdownName,
          question,
          previousHistory,
          approvedDocument,
        );

      /*
       * BACKEND FOUND EVIDENCE
       *
       * Normal answer from the current document
       * or from an already approved document.
       */
      if (response.status === "ANSWER") {
        setMessages((current) => [
          ...current,
          {
            role: "assistant",
            content: response.answer,
          },
        ]);

        return;
      }

      /*
       * BACKEND FOUND ANOTHER RELEVANT DOCUMENT
       *
       * Do NOT answer yet.
       *
       * The user must explicitly approve the
       * document first.
       */
      if (
        response.status ===
        "CROSS_DOCUMENT_PERMISSION"
      ) {
        setPermissionDocument(
          response.suggestedDocument,
        );

        return;
      }

      /*
       * No sufficient evidence anywhere
       * in the knowledge library.
       */
      if (
        response.status === "NO_EVIDENCE"
      ) {
        setMessages((current) => [
          ...current,
          {
            role: "assistant",
            content:
              response.answer ||
              "I could not find sufficient information in the OKF knowledge library to answer this question.",
          },
        ]);

        return;
      }

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

  /*
   * User clicks YES.
   *
   * We remember the approved document and
   * immediately send the same question again.
   *
   * The backend now receives approvedDocument
   * and is allowed to retrieve information from it.
   */
  async function handleApproveDocument() {
    if (!permissionDocument || loading) {
      return;
    }

    const documentToApprove =
      permissionDocument;

    setApprovedDocument(
      documentToApprove,
    );

    setPermissionDocument(undefined);
    setError("");
    setLoading(true);

    /*
     * The last user message is the question
     * that caused the permission request.
     */
    const lastUserMessage =
      [...messages]
        .reverse()
        .find(
          (message) =>
            message.role === "user",
        );

    if (!lastUserMessage) {
      setLoading(false);
      return;
    }

    try {
      const response =
        await chatWithDocument(
          markdownName,
          lastUserMessage.content,
          messages.slice(0, -1),
          documentToApprove,
        );

      if (
        response.status === "ANSWER"
      ) {
        setMessages((current) => [
          ...current,
          {
            role: "assistant",
            content:
              response.answer,
          },
        ]);
        setApprovedDocument(undefined);
      } else {
        setMessages((current) => [
          ...current,
          {
            role: "assistant",
            content:
              response.answer ||
              "I could not find sufficient information in the approved document.",
          },
        ]);
      }

    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Could not retrieve the approved document.",
      );
    } finally {
      setLoading(false);
    }
  }

  /*
   * User clicks NO.
   *
   * We do not call the backend again.
   * Therefore the other document is never retrieved.
   */
  function handleDenyDocument() {
    setPermissionDocument(undefined);

    setMessages((current) => [
      ...current,
      {
        role: "assistant",
        content:
          "Okay. I will not use that other document to answer this question.",
      },
    ]);
  }

  return (
    <section className="mt-6 rounded-2xl border bg-card p-4 shadow-soft">

      {/* Header */}
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
              Ask questions about{" "}
              {markdownName}
            </p>
          </div>

        </div>

        <button
          type="button"
          onClick={() =>
            setIsOpen(
              (value) => !value,
            )
          }
          className="rounded-xl border px-3 py-2 text-sm font-medium hover:bg-muted"
        >
          {isOpen
            ? "Hide chat"
            : "Open chat"}
        </button>

      </div>

      {isOpen && (
        <div className="mt-4 space-y-4">

          {/* Chat messages */}
          <div
            className="max-h-80 min-h-24 space-y-3 overflow-y-auto rounded-xl border bg-background p-3"
            aria-live="polite"
          >

            {messages.length === 0 && (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Ask your first question about this document.
              </p>
            )}

            {messages.map(
              (message, index) => (
                <div
                  key={`${index}-${message.role}`}
                  className={`flex ${
                    message.role ===
                    "user"
                      ? "justify-end"
                      : "justify-start"
                  }`}
                >

                  <div
                    className={`max-w-[90%] whitespace-pre-wrap break-words rounded-2xl px-3 py-2.5 text-sm ${
                      message.role ===
                      "user"
                        ? "bg-primary text-primary-foreground"
                        : "border bg-muted"
                    }`}
                  >

                    <p className="mb-1 text-xs font-semibold opacity-75">
                      {message.role ===
                      "user"
                        ? "You"
                        : "OKF Assistant"}
                    </p>

                    {message.content}

                  </div>

                </div>
              ),
            )}

            {/* Loading */}
            {loading && (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <LoaderCircle className="h-4 w-4 animate-spin" />

                Generating answer...
              </div>
            )}

          </div>

          {/* Cross-document permission request */}
          {permissionDocument && (
            <div className="rounded-xl border border-primary/30 bg-primary/5 p-4">

              <div className="flex gap-3">

                <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-primary text-primary-foreground">
                  <FileText className="h-5 w-5" />
                </div>

                <div className="min-w-0 flex-1">

                  <p className="text-sm font-semibold">
                    This question may belong to another document.
                  </p>

                  <p className="mt-1 text-sm text-muted-foreground">
                    I found potentially relevant
                    information in:
                  </p>

                  <p className="mt-2 break-words text-sm font-semibold">
                    {permissionDocument}
                  </p>

                  <p className="mt-2 text-sm text-muted-foreground">
                    Would you like me to use this
                    document to answer your question?
                  </p>

                  <div className="mt-3 flex flex-wrap gap-2">

                    <button
                      type="button"
                      onClick={
                        handleApproveDocument
                      }
                      disabled={loading}
                      className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <Check className="h-4 w-4" />
                      Yes, use this document
                    </button>

                    <button
                      type="button"
                      onClick={
                        handleDenyDocument
                      }
                      disabled={loading}
                      className="inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-semibold hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      <X className="h-4 w-4" />
                      No
                    </button>

                  </div>

                </div>

              </div>

            </div>
          )}

          {/* Error */}
          {error && (
            <p
              role="alert"
              className="rounded-xl border border-destructive/30 p-3 text-sm text-destructive"
            >
              {error}
            </p>
          )}

          {/* Input */}
          <form
            onSubmit={
              handleSubmit
            }
            className="flex items-end gap-2"
          >

            <textarea
              value={input}
              onChange={(event) =>
                setInput(
                  event.target.value,
                )
              }
              onKeyDown={(event) => {

                if (
                  event.key ===
                    "Enter" &&
                  !event.shiftKey &&
                  !event.nativeEvent
                    .isComposing
                ) {
                  event.preventDefault();

                  event.currentTarget
                    .form
                    ?.requestSubmit();
                }

              }}
              placeholder="Ask a question about this document..."
              rows={2}
              disabled={loading}
              className="min-w-0 flex-1 resize-y rounded-xl border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring disabled:opacity-60"
            />

            <button
              type="submit"
              disabled={
                loading ||
                !input.trim()
              }
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
            Answers are generated locally using Ollama and
            approved OKF knowledge documents.
          </p>

        </div>
      )}

    </section>
  );
}