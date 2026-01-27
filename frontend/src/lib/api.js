export const API_URL = import.meta.env.VITE_API_URL || '/api';

export const isApiSuccess = (result) => {
  if (!result || typeof result !== 'object') return false;
  if (typeof result.code === 'number') return result.code === 0;
  if (typeof result.success === 'boolean') return result.success;
  return false;
};
