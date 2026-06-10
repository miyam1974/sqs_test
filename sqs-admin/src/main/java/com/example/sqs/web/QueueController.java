package com.example.sqs.web;

import com.example.sqs.service.QueueService;
import com.example.sqs.web.dto.QueueEditForm;
import com.example.sqs.web.dto.QueueForm;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Controller
@Profile("web")
@RequestMapping("/queues")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("queues", queueService.listQueues());
        return "queues/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("queueForm", new QueueForm());
        model.addAttribute("existingQueues", queueService.listQueues());
        model.addAttribute("deduplicationScopes", com.example.sqs.model.DeduplicationScopeOption.values());
        model.addAttribute("fifoThroughputLimits", com.example.sqs.model.FifoThroughputLimitOption.values());
        model.addAttribute("redrivePermissions", com.example.sqs.model.RedrivePermissionOption.values());
        model.addAttribute("sseModes", com.example.sqs.model.SseModeOption.values());
        return "queues/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("queueForm") QueueForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("existingQueues", queueService.listQueues());
            model.addAttribute("deduplicationScopes", com.example.sqs.model.DeduplicationScopeOption.values());
            model.addAttribute("fifoThroughputLimits", com.example.sqs.model.FifoThroughputLimitOption.values());
            model.addAttribute("redrivePermissions", com.example.sqs.model.RedrivePermissionOption.values());
            model.addAttribute("sseModes", com.example.sqs.model.SseModeOption.values());
            return "queues/form";
        }
        try {
            String url = queueService.createQueue(form);
            redirectAttributes.addFlashAttribute("success", "キューを作成しました: " + form.resolvedQueueName());
            redirectAttributes.addAttribute("encodedUrl", encodeUrl(url));
            return "redirect:/queues/{encodedUrl}";
        } catch (Exception e) {
            model.addAttribute("queueForm", form);
            model.addAttribute("existingQueues", queueService.listQueues());
            model.addAttribute("deduplicationScopes", com.example.sqs.model.DeduplicationScopeOption.values());
            model.addAttribute("fifoThroughputLimits", com.example.sqs.model.FifoThroughputLimitOption.values());
            model.addAttribute("redrivePermissions", com.example.sqs.model.RedrivePermissionOption.values());
            model.addAttribute("sseModes", com.example.sqs.model.SseModeOption.values());
            model.addAttribute("error", "作成に失敗しました: " + e.getMessage());
            return "queues/form";
        }
    }

    @GetMapping("/{encodedUrl}")
    public String detail(@PathVariable String encodedUrl, Model model) {
        String queueUrl = decodeUrl(encodedUrl);
        model.addAttribute("queue", queueService.getQueue(queueUrl));
        return "queues/detail";
    }

    @GetMapping("/{encodedUrl}/edit")
    public String editForm(@PathVariable String encodedUrl, Model model) {
        String queueUrl = decodeUrl(encodedUrl);
        model.addAttribute("queueEditForm", queueService.toEditForm(queueUrl));
        return "queues/edit";
    }

    @PostMapping("/{encodedUrl}/edit")
    public String update(@PathVariable String encodedUrl,
                         @Valid @ModelAttribute("queueEditForm") QueueEditForm form,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "queues/edit";
        }
        form.setQueueUrl(decodeUrl(encodedUrl));
        try {
            queueService.updateQueue(form);
            redirectAttributes.addFlashAttribute("success", "キュー属性を更新しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "更新に失敗しました: " + e.getMessage());
        }
        return "redirect:/queues/" + encodedUrl;
    }

    @PostMapping("/{encodedUrl}/delete")
    public String delete(@PathVariable String encodedUrl, RedirectAttributes redirectAttributes) {
        try {
            queueService.deleteQueue(decodeUrl(encodedUrl));
            redirectAttributes.addFlashAttribute("success", "キューを削除しました");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "削除に失敗しました: " + e.getMessage());
            return "redirect:/queues/" + encodedUrl;
        }
        return "redirect:/queues";
    }

    static String encodeUrl(String url) {
        return java.net.URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    static String decodeUrl(String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }
}
