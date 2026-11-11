package com.se201411.project.mooncake.feedback.dto;

import com.se201411.project.mooncake.entity.Feedback;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FeedbackForm {

    @NotNull(message = "Vui lòng chọn số sao.")
    @Min(value = 1, message = "Số sao từ 1 đến 5.")
    @Max(value = 5, message = "Số sao từ 1 đến 5.")
    private Integer rating;

    @NotBlank(message = "Vui lòng nhập nội dung góp ý.")
    @Size(max = 2000, message = "Góp ý tối đa 2.000 ký tự.")
    private String content;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void dtoToEntity(Feedback feedback) {
        feedback.setRating(rating);
        feedback.setContent(content.trim());
    }
}
