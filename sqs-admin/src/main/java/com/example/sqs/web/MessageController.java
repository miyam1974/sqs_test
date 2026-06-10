package com.example.sqs.web;

import com.example.sqs.service.MessageSendService;
import com.example.sqs.service.QueueService;
import com.example.sqs.web.dto.SendMessageForm;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Profile("web")
@RequestMapping("/messages")
public class MessageController {

    private final QueueService queueService;
    private final MessageSendService messageSendService;

    public MessageController(QueueService queueService, MessageSendService messageSendService) {
        this.queueService = queueService;
        this.messageSendService = messageSendService;
    }

    @GetMapping("/send")
    public String sendForm(@RequestParam(required = false) String queueUrl, Model model) {
        model.addAttribute("queues", queueService.listQueues());
        SendMessageForm form = new SendMessageForm();
        if (queueUrl != null && !queueUrl.isBlank()) {
            form.setQueueUrl(queueUrl);
        }
        model.addAttribute("sendForm", form);
        return "messages/send";
    }

    @PostMapping("/send")
    public String send(@Valid @ModelAttribute("sendForm") SendMessageForm form,
                       BindingResult result,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        model.addAttribute("queues", queueService.listQueues());
        if (result.hasErrors()) {
            return "messages/send";
        }
        try {
            var response = messageSendService.send(form);
            redirectAttributes.addFlashAttribute("success",
                    "送信しました (MessageId: " + response.messageId() + ")");
            redirectAttributes.addAttribute("queueUrl", form.getQueueUrl());
            return "redirect:/messages/send";
        } catch (Exception e) {
            model.addAttribute("error", "送信に失敗しました: " + e.getMessage());
            return "messages/send";
        }
    }
}
