package com.example.test.filter;

import com.bdp.idmapping.common.Constant;
import com.bdp.idmapping.manager.IdRelationRedisManager;
import com.bdp.idmapping.response.Response;
import com.example.test.config.ApplicationConfig;
import com.example.test.utils.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @Auther: CAI
 * @Date: 2022/11/9 - 11 - 09 - 23:58
 * @Description: com.example.test.filter
 * @version: 1.0
 */
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    @Autowired
    private IdRelationRedisManager idRelationRedisManager;

    @Autowired
    private ApplicationConfig applicationConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuthenticationFilter initialized,authentication config");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String requestUrl = ((HttpServletRequest) request).getRequestURI();
        if (requestUrl.contains("/hid/relation/mapping")) {
            String bizName = request.getParameter("bizName");
            if (applicationConfig.isOpenBizNameValidate() && !applicationConfig.getBizNameSet().contains(bizName)) {
                ResponseUtils.write(response, Response.fail(Constant.CODE_BIZNAME_INVALID, Constant.CODE_BIZNAME_INVALID_MSG));
                return;
            }
            int maxFlow = applicationConfig.getMaxFlow();
            if (idRelationRedisManager.isOverLimit(bizName, maxFlow)) {
                ResponseUtils.write(response, Response.fail(Constant.CODE_SERVER_FLOW_SPILL, Constant.CODE_SERVER_FLOW_SPILL_MSG));
                logger.info("Current limiting:{}", bizName);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("AuthenticationFilter destroy!");
    }
}
