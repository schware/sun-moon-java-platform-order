package com.sunmoon.platform.domain.businessday;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Every other test here is a {@code @WebMvcTest} slice with the service
 * mocked, so nothing exercised the real bean definitions — and a service
 * with two constructors and no {@code @Autowired} passed the whole suite
 * and then failed to start in production. This is the cheapest thing
 * that would have caught it: no database, no web layer, just "can Spring
 * build this".
 */
class BusinessDayServiceWiringTest {

    @Test
    void springCanConstructTheService() {
        new ApplicationContextRunner()
                .withBean(BusinessDayRepository.class, () -> mock(BusinessDayRepository.class))
                .withBean(BusinessDayService.class)
                .run(context -> assertThat(context).hasNotFailed()
                        .hasSingleBean(BusinessDayService.class));
    }
}
