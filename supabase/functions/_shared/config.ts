export const SERVER_ONLY_ENV_NAMES = [
  "SUPABASE_SECRET_KEY",
  "SUPABASE_SERVICE_ROLE_KEY",
] as const;

export interface EnvironmentReader {
  get(name: string): string | undefined;
}

export function requiredEnvironment(
  environment: EnvironmentReader,
  name: string,
): string {
  const value = environment.get(name)?.trim();
  if (!value) throw new Error(`Required server environment is missing: ${name}`);
  return value;
}

export function publicConfiguration(environment: EnvironmentReader): {
  supabaseUrl: string;
} {
  return { supabaseUrl: requiredEnvironment(environment, "SUPABASE_URL") };
}
