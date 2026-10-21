package com.se201411.project.mooncake.cart.controller;

import com.se201411.project.mooncake.cart.dto.CustomBoxEditView;
import com.se201411.project.mooncake.cart.dto.CustomBoxForm;
import com.se201411.project.mooncake.cart.dto.CustomBoxPageView;
import com.se201411.project.mooncake.cart.dto.CustomCakeOptionView;
import com.se201411.project.mooncake.cart.dto.CustomCakeQuantity;
import com.se201411.project.mooncake.cart.dto.CustomCakeSelectionForm;
import com.se201411.project.mooncake.cart.dto.SaveCustomBoxCommand;
import com.se201411.project.mooncake.cart.service.CustomBoxService;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.security.dto.ShopperIdentity;
import com.se201411.project.mooncake.security.service.CurrentCustomerService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/custom-boxes")
public class CustomBoxController {

    private final CustomBoxService customBoxService;
    private final CurrentCustomerService currentCustomerService;

    public CustomBoxController(
        CustomBoxService customBoxService,
        CurrentCustomerService currentCustomerService
    ) {
        this.customBoxService = customBoxService;
        this.currentCustomerService = currentCustomerService;
    }

    @GetMapping
    public String createForm(Model model) {
        currentCustomerService.requireCurrentCustomer();
        CustomBoxPageView builder = customBoxService.getBuilderView();
        if (!model.containsAttribute("customBoxForm")) {
            model.addAttribute("customBoxForm", createEmptyForm(builder));
        }
        addPageModel(model, builder, false, null);
        return "custom-box";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("customBoxForm") CustomBoxForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (bindingResult.hasErrors()) {
            addPageModel(model, customBoxService.getBuilderView(), false, null);
            return "custom-box";
        }

        try {
            customBoxService.createAndAddToCart(customer, toCommand(form));
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã thêm hộp quà tự chọn vào giỏ hàng"
            );
            return "redirect:/cart";
        } catch (MoonCakeException exception) {
            bindingResult.reject("customBox", exception.getMessage());
            addPageModel(model, customBoxService.getBuilderView(), false, null);
            return "custom-box";
        }
    }

    @GetMapping("/{configurationId}/edit")
    public String editForm(
        @PathVariable long configurationId,
        Model model
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        CustomBoxPageView builder = customBoxService.getBuilderView();
        CustomBoxEditView editView = customBoxService.getConfigurationForEdit(
            customer,
            configurationId
        );
        model.addAttribute("customBoxForm", createEditForm(builder, editView));
        addPageModel(model, builder, true, configurationId);
        return "custom-box";
    }

    @PostMapping("/{configurationId}")
    public String update(
        @PathVariable long configurationId,
        @Valid @ModelAttribute("customBoxForm") CustomBoxForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        ShopperIdentity customer = currentCustomerService.requireCurrentCustomer();
        if (bindingResult.hasErrors()) {
            addPageModel(model, customBoxService.getBuilderView(), true, configurationId);
            return "custom-box";
        }

        try {
            customBoxService.updateConfiguration(customer, configurationId, toCommand(form));
            redirectAttributes.addFlashAttribute(
                "successMessage",
                "Đã cập nhật hộp quà tự chọn"
            );
            return "redirect:/cart";
        } catch (MoonCakeException exception) {
            bindingResult.reject("customBox", exception.getMessage());
            addPageModel(model, customBoxService.getBuilderView(), true, configurationId);
            return "custom-box";
        }
    }

    private CustomBoxForm createEmptyForm(CustomBoxPageView builder) {
        CustomBoxForm form = new CustomBoxForm();
        if (!builder.boxes().isEmpty()) {
            form.setBoxId(builder.boxes().getFirst().boxId());
        }
        form.setCakes(createCakeForms(builder, Map.of()));
        return form;
    }

    private CustomBoxForm createEditForm(
        CustomBoxPageView builder,
        CustomBoxEditView editView
    ) {
        CustomBoxForm form = new CustomBoxForm();
        form.setBoxId(editView.boxId());
        form.setBoxQuantity(editView.boxQuantity());
        form.setCakes(createCakeForms(builder, editView.cakeQuantities()));
        return form;
    }

    private List<CustomCakeSelectionForm> createCakeForms(
        CustomBoxPageView builder,
        Map<Long, Integer> selectedQuantities
    ) {
        List<CustomCakeSelectionForm> forms = new ArrayList<>();
        for (CustomCakeOptionView cake : builder.cakes()) {
            CustomCakeSelectionForm selection = new CustomCakeSelectionForm();
            selection.setVariantId(cake.variantId());
            selection.setQuantity(selectedQuantities.getOrDefault(cake.variantId(), 0));
            forms.add(selection);
        }
        return forms;
    }

    private SaveCustomBoxCommand toCommand(CustomBoxForm form) {
        List<CustomCakeQuantity> cakes = new ArrayList<>();
        for (CustomCakeSelectionForm selection : form.getCakes()) {
            cakes.add(new CustomCakeQuantity(
                selection.getVariantId(),
                selection.getQuantity()
            ));
        }
        return new SaveCustomBoxCommand(
            form.getBoxId(),
            form.getBoxQuantity(),
            cakes
        );
    }

    private void addPageModel(
        Model model,
        CustomBoxPageView builder,
        boolean editing,
        Long configurationId
    ) {
        model.addAttribute("builder", builder);
        model.addAttribute("editing", editing);
        model.addAttribute("configurationId", configurationId);
    }
}
