package com.se201411.project.mooncake.cart.service;

import com.se201411.project.mooncake.cart.dto.CustomBoxEditView;
import com.se201411.project.mooncake.cart.dto.CustomBoxPageView;
import com.se201411.project.mooncake.cart.dto.SaveCustomBoxCommand;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;

public interface CustomBoxService {

    CustomBoxPageView getBuilderView();

    CustomBoxEditView getConfigurationForEdit(
        ShopperIdentity customerIdentity,
        long configurationId
    );

    long createAndAddToCart(
        ShopperIdentity customerIdentity,
        SaveCustomBoxCommand command
    );

    void updateConfiguration(
        ShopperIdentity customerIdentity,
        long configurationId,
        SaveCustomBoxCommand command
    );
}
