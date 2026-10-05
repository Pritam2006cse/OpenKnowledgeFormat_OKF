<!-- LOVABLE:BEGIN -->
> [!IMPORTANT]
> This project is connected to [Lovable](https://lovable.dev). Avoid rewriting
> published git history — force pushing, or rebasing/amending/squashing commits
> that are already pushed — as it rewrites history on Lovable's side and the
> user will likely lose their project history.
>
> Commits you push to the connected branch sync back to Lovable and show up in
> the editor, so keep the branch in a working state.
<!-- LOVABLE:END -->

# AGENTS

- All backend calls go through `src/lib/okf/api.ts` (currently mocked); swap bodies for real `/upload`, `/process`, `/knowledge`, `/search` fetches without changing signatures — keeps UI decoupled from the Java backend.
- Shared app state lives in `src/lib/okf/store.ts` (useSyncExternalStore) so Upload, Knowledge and Search screens read the same data.
- Global navigation and the one-time intro live in `AppShell`, wrapped around `<Outlet />` in `__root.tsx`; each screen is its own route.
- Overlays (drawers/modals) must portal to `document.body`, because the animated page container creates a containing block that clips fixed elements.
