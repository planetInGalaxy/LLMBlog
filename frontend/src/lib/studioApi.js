const STUDIO_AUTH_MESSAGE = '登录已过期，请重新登录';
const STUDIO_SERVER_ERROR_MESSAGE = '服务器开小差了，请稍后再试';

export const handleStudioWriteResponse = async (response, navigate) => {
  if (response.status === 401 || response.status === 403) {
    alert(STUDIO_AUTH_MESSAGE);
    localStorage.removeItem('token');
    navigate('/studio/login');
    return null;
  }
  if (response.status >= 500) {
    alert(STUDIO_SERVER_ERROR_MESSAGE);
    return null;
  }
  return response.json();
};
