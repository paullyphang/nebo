package cn.pengbo.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pengbo on 2016/8/8.
 */
public class ProtocolRouterFactory {
    public static List<ProtocolRouter> protocolRouters = new ArrayList<ProtocolRouter>();

    static {
        protocolRouters.add(new ThriftProtocolRouter());
    }
}
