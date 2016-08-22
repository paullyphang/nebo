package io.nebo.hessian;

import com.caucho.hessian.server.HessianSkeleton;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pengbo on 2016/7/18.
 */
public class HessianFilter implements Filter {
    private static Log logger = LogFactory.getLog(HessianFilter.class);
    private Map<String, HessianSkeleton> hessianSkeletonMap = new HashMap<String, HessianSkeleton>();

    public HessianFilter(ServletContext context) {
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.findWebApplicationContext(context);
        String[] strarr = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(webApplicationContext, Object.class);
        for (String s : strarr) {
            Object target = webApplicationContext.getBean(s);
            HessianEndpoint hessianEndpointAnnotation = target.getClass().getAnnotation(HessianEndpoint.class);
            if (hessianEndpointAnnotation != null) {
                try {
                    Class apiClz = null;
                    Class[] interfacesClass = target.getClass().getInterfaces();
                    if (interfacesClass != null && interfacesClass.length > 0) {
                        apiClz = interfacesClass[0];
                    } else {
                        apiClz = target.getClass();
                    }
                    HessianSkeleton hessianSkeleton = new HessianSkeleton(target, apiClz);
                    hessianSkeletonMap.put(HessianConstant.HESSIAN_PATH + hessianEndpointAnnotation.servicePattern(), hessianSkeleton);
                } catch (Exception e) {
                    logger.error("registerProcessor error : " + e.getMessage(), e);
                }
            }

        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String urlPattern = httpServletRequest.getRequestURI();
        HessianSkeleton hessianSkeleton = hessianSkeletonMap.get(urlPattern);
        try {
            if (hessianSkeleton != null) {
                hessianSkeleton.invoke(request.getInputStream(), response.getOutputStream());
            } else {
                chain.doFilter(request,response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {

    }


    private void registerSrv() {

    }
}
