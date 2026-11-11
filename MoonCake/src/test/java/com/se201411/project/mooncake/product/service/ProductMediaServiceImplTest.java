package com.se201411.project.mooncake.product.service;

import com.se201411.project.mooncake.product.service.ProductAdminService;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.entity.Product;
import com.se201411.project.mooncake.entity.ProductVideo;
import com.se201411.project.mooncake.product.repository.ProductRepository;
import com.se201411.project.mooncake.product.repository.ProductVideoRepository;
import com.se201411.project.mooncake.product.service.impl.ProductMediaServiceImpl;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductMediaServiceImplTest {

    private final ProductVideoRepository productVideoRepository = mock(ProductVideoRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final AdminAccessService adminAccessService = mock(AdminAccessService.class);
    private final ProductMediaService service = new ProductMediaServiceImpl(
        productVideoRepository,
        productRepository,
        adminAccessService,
        mock(ProductAdminService.class)
    );

    @Test
    void addsValidVideoForExistingProduct() {
        Product product = new Product();
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));

        service.addVideo(3L, "https://example.test/cake.mp4", "  Cắt bánh  ");

        ArgumentCaptor<ProductVideo> saved = ArgumentCaptor.forClass(ProductVideo.class);
        verify(productVideoRepository).save(saved.capture());
        assertThat(saved.getValue().getProduct()).isSameAs(product);
        assertThat(saved.getValue().getCaption()).isEqualTo("Cắt bánh");
    }

    @Test
    void rejectsVideoThatIsNotMp4OrWebm() {
        assertThatThrownBy(() -> service.addVideo(3L, "https://example.test/cake.avi", "Video"))
            .isInstanceOf(BusinessRuleViolationException.class);
        verify(productVideoRepository, never()).save(any());
    }
}
