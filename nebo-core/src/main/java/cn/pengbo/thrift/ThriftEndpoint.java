package cn.pengbo.thrift;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by pengbo on 2016/6/30.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ThriftEndpoint {
    String serviceName() default "";
}
