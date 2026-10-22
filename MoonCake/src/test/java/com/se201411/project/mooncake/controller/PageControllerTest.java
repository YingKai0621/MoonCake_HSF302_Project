package com.se201411.project.mooncake.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageControllerTest {

    private final PageController controller = new PageController();

    @Test
    void rendersHomepage() {
        assertThat(controller.homepage()).isEqualTo("homepage");
    }

    @Test
    void rendersBusinessPage() {
        assertThat(controller.business()).isEqualTo("business");
    }
}
