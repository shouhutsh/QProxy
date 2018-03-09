package cn.edu.zzti.qi.util;

import cn.edu.zzti.qi.exception.BizException;
import cn.edu.zzti.qi.model.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpUtils {

    private static final Logger logger = LogManager.getLogger(HttpUtils.class);

    public static final HttpRequest parseRequest(final byte[] data) throws BizException {
        int offset = 0;
        HttpRequest model = new HttpRequest();
        for (RequestStruct s : RequestStruct.values()) {
            offset = s.parse(offset, data, model);
        }
        return model;
    }

    public static final byte[] requestAssemble(HttpRequest model) {
        StringBuilder sb = new StringBuilder();
        sb.append(model.getMethod()).append(' ');
        sb.append(model.getPath()).append(' ');
        sb.append(model.getVersion()).append("\r\n");
        for (Map.Entry<String, String> e : model.getHeaders().entrySet()) {
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        byte[] httpHead = sb.toString().getBytes();
        int length = httpHead.length + model.getBody().length;
        byte[] buffer = new byte[length];
        System.arraycopy(httpHead, 0, buffer, 0, httpHead.length);
        System.arraycopy(model.getBody(), 0, buffer, httpHead.length, model.getBody().length);
        return buffer;
    }

    private enum RequestStruct {
        METHOD {
            @Override
            protected int parse(int offset, byte[] data, HttpRequest model) throws BizException {
                for (int i = offset; i < data.length; ++i) {
                    if (' ' == data[i]) {
                        byte[] method = new byte[i - offset];
                        System.arraycopy(data, offset, method, 0, method.length);
                        model.setMethod(new String(method));
                        return i + 1;
                    }
                }
                throw new BizException("解析 METHOD 失败");
            }
        },
        PATH {
            @Override
            protected int parse(int offset, byte[] data, HttpRequest model) throws BizException {
                for (int i = offset; i < data.length; ++i) {
                    if (' ' == data[i]) {
                        byte[] path = new byte[i - offset];
                        System.arraycopy(data, offset, path, 0, path.length);
                        model.setPath(new String(path));
                        return i + 1;
                    }
                }
                throw new BizException("解析 PATH 失败");
            }
        },
        VERSION {
            @Override
            protected int parse(int offset, byte[] data, HttpRequest model) throws BizException {
                for (int i = offset; i < data.length; ++i) {
                    if ('\r' == data[i] && '\n' == data[i + 1]) {
                        byte[] version = new byte[i - offset];
                        System.arraycopy(data, offset, version, 0, version.length);
                        model.setVersion(new String(version));
                        return i + 2;
                    }
                }
                throw new BizException("解析 VERSION 失败");
            }
        },
        HEADERS {
            @Override
            protected int parse(int offset, byte[] data, HttpRequest model) throws BizException {
                int temp = offset;
                Map<String, String> headers = new LinkedHashMap<>();
                for (int i = offset; i < data.length; ) {
                    if ('\r' == data[i] && '\n' == data[i + 1]) {
                        model.setHeaders(headers);
                        return i + 2;
                    }
                    while (++i <= data.length) {
                        if ('\r' == data[i] && '\n' == data[i + 1]) {
                            byte[] bytes = new byte[i - temp];
                            System.arraycopy(data, temp, bytes, 0, bytes.length);
                            String header = new String(bytes);
                            int split = header.indexOf(':');
                            headers.put(
                                    header.substring(0, split).trim().toLowerCase(),
                                    header.substring(split + 1, header.length()).trim());
                            temp = i += 2;
                            break;
                        }
                    }
                }
                throw new BizException("解析 HEADERS 失败");
            }
        },
        BODY {
            @Override
            protected int parse(int offset, byte[] data, HttpRequest model) throws BizException {
                byte[] body = new byte[data.length - offset];
                System.arraycopy(data, offset, body, 0, body.length);
                model.setBody(body);
                return offset + 1;
            }
        };

        protected abstract int parse(int offset, byte[] data, HttpRequest model) throws BizException;
    }

    public static String getHost(String host) {
        return host.split(":")[0];
    }

    public static int getPort(String host) {
        String[] kv = host.split(":");
        if (2 == kv.length) {
            return Integer.valueOf(kv[1]);
        }
        return 80;
    }
}
