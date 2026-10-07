package com.examly.springapp.service;

import com.examly.springapp.dto.FeedbackDTO;
import com.examly.springapp.model.Feedback;
import com.examly.springapp.repository.FeedbackRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackRepo feedbackRepo;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AiService aiService;

    @Override
    public FeedbackDTO createFeedback(FeedbackDTO feedbackDto) {
        Feedback feedback = modelMapper.map(feedbackDto, Feedback.class);
        if (feedback.getDate() == null) {
            feedback.setDate(LocalDate.now());
        }
        try {
            // AI sentiment analysis + auto-tagging; feedback is still saved if the analysis fails.
            aiService.applyAnalysis(feedback);
        } catch (Exception ignored) {
            // keep the feedback without AI attributes
        }
        return modelMapper.map(feedbackRepo.save(feedback), FeedbackDTO.class);
    }

    @Override
    public FeedbackDTO getFeedbackById(Long feedbackId) {
        return feedbackRepo.findById(feedbackId).map(feedback -> modelMapper.map(feedback, FeedbackDTO.class)).orElse(null);
    }

    @Override
    public List<FeedbackDTO> getAllFeedbacks() {
        return toDtos(feedbackRepo.findAll());
    }

    @Override
    public FeedbackDTO deleteFeedback(Long feedbackId) {
        Optional<Feedback> feedbackOpt = feedbackRepo.findById(feedbackId);
        if (feedbackOpt.isPresent()) {
            feedbackRepo.delete(feedbackOpt.get());
            return modelMapper.map(feedbackOpt.get(), FeedbackDTO.class);
        }
        return null;
    }

    @Override
    public List<FeedbackDTO> getFeedbacksByUserId(Long userId) {
        return toDtos(feedbackRepo.findByUserUserId(userId));
    }

    private List<FeedbackDTO> toDtos(List<Feedback> feedbacks) {
        return feedbacks.stream().map(feedback -> modelMapper.map(feedback, FeedbackDTO.class)).collect(Collectors.toList());
    }
}
