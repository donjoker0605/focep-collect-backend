package org.example.collectfocep.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuditConfig {
    // Cette configuration active automatiquement les aspects AOP
    // proxyTargetClass = true force l'utilisation de CGLIB pour les proxies
}