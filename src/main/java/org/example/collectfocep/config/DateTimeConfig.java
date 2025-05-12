package org.example.collectfocep.config;

import org.example.collectfocep.constants.AppConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

import java.time.format.DateTimeFormatter;

@Configuration
public class DateTimeConfig {

    @Bean
    public FormattingConversionService conversionService() {
        DefaultFormattingConversionService conversionService =
                new DefaultFormattingConversionService(false);

        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT));
        registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern(AppConstants.DATETIME_FORMAT));
        registrar.registerFormatters(conversionService);

        return conversionService;
    }
}
