import type { ReactNode } from "react";
import type { AsyncState } from "../hooks/useApi";
import { ErrorState, Skeleton } from "./States";

interface Props<T> {
  state: AsyncState<T>;
  children: (data: T) => ReactNode;
  /** Shown instead of `children` when the data has nothing to show. */
  empty?: ReactNode;
  isEmpty?: (data: T) => boolean;
  loading?: ReactNode;
}

/**
 * Renders a useApi result: placeholders until the first result, an error with a
 * retry if that fails, then the data. Data that already loaded stays on screen
 * when a later refresh fails, so polling pages don't flash an error.
 */
export function AsyncContent<T>({
  state,
  children,
  empty,
  isEmpty = (data) => Array.isArray(data) && data.length === 0,
  loading = <Skeleton />,
}: Props<T>) {
  if (state.data === null) {
    if (state.error) return <ErrorState message={state.error} onRetry={state.refetch} />;
    return <>{loading}</>;
  }
  if (empty !== undefined && isEmpty(state.data)) return <>{empty}</>;
  return <>{children(state.data)}</>;
}
