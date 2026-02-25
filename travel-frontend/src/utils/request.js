import axios from 'axios';
import { ElMessage } from 'element-plus';
import router from '../router';

// 创建 axios 实例，配置基础路径和超时时间
const request = axios.create({
    baseURL: 'http://localhost:8080', // 如果你的后端端口不同，请在此修改
    timeout: 10000
});

// 请求拦截器 (Request Interceptor)
request.interceptors.request.use(config => {
    config.headers['Content-Type'] = 'application/json;charset=utf-8';

    // 自动在每次请求头中携带 JWT Token
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = 'Bearer ' + token;
    }
    return config;
}, error => {
    return Promise.reject(error);
});

// 响应拦截器 (Response Interceptor)
request.interceptors.response.use(
    response => {
        let res = response.data;
        // 处理文件流
        if (response.config.responseType === 'blob') {
            return res;
        }
        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res;
        }

        // 统一业务错误处理
        if (res.code !== '200') {
            ElMessage.error(res.msg || '系统异常');

            // 401 未登录或 Token 过期，强制清除本地脏数据并返回登录页
            if (res.code === '401') {
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                router.push('/login');
            }
        }
        return res;
    },
    error => {
        console.error('网络请求错误:', error);
        ElMessage.error('网络请求异常，请稍后重试');
        return Promise.reject(error);
    }
);

export default request;