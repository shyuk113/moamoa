package com.moamoa.config;

import java.time.Duration;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.*;

@Configuration
public class LocaleConfig implements WebMvcConfigurer {
  @Bean
  public LocaleResolver localeResolver() {
    var r = new CookieLocaleResolver("MOAMOA_LANG");
    r.setDefaultLocale(Locale.KOREAN);
    r.setCookieMaxAge(Duration.ofDays(365));
    r.setCookieHttpOnly(true);
    r.setCookieSameSite("Lax");
    return r;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(
        new org.springframework.web.servlet.HandlerInterceptor() {
          @Override
          public boolean preHandle(
              jakarta.servlet.http.HttpServletRequest req,
              jakarta.servlet.http.HttpServletResponse res,
              Object handler) {
            String lang = req.getParameter("lang");
            if ("ko".equals(lang) || "en".equals(lang))
              org.springframework.web.servlet.support.RequestContextUtils.getLocaleResolver(req)
                  .setLocale(req, res, Locale.forLanguageTag(lang));
            return true;
          }
        });
  }
}
