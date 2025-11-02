package com.ktb.community.service;

import com.ktb.community.dto.request.CreateCommentRequestDto;
import com.ktb.community.dto.request.UpdateCommentRequestDto;
import com.ktb.community.dto.response.CommentResponseDto;
import com.ktb.community.dto.response.CrudCommentResponseDto;
import com.ktb.community.dto.response.CursorCommentResponseDto;
import com.ktb.community.entity.Comment;
import com.ktb.community.entity.Post;
import com.ktb.community.entity.User;
import com.ktb.community.exception.custom.CommentNotFoundException;
import com.ktb.community.exception.custom.PostNotFoundException;
import com.ktb.community.exception.custom.UnauthorizedException;
import com.ktb.community.exception.custom.UserNotFoundException;
import com.ktb.community.jwt.JwtUtil;
import com.ktb.community.repository.CommentRepository;
import com.ktb.community.repository.PostRepository;
import com.ktb.community.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class CommentService {
    CommentRepository commentRepository;
    PostRepository postRepository;
    UserRepository userRepository;
    JwtUtil jwtUtil;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository, JwtUtil jwtUtil) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }


    public CursorCommentResponseDto<CommentResponseDto> getCommentList(Long postId, Long cursor, int size, String token) {
        log.info("===== 댓글 목록 조회 요청 =====");
        log.info("postId: {}, cursor: {}, size: {}", postId, cursor, size);

        Long userId = this.jwtUtil.extractUserIdFromToken(token);
        log.info("요청 userId: {}", userId);

        List<Comment> comments;
        Pageable pageable = PageRequest.of(0, size + 1);

        if (cursor == null) {
            // cursor가 null이라면 첫 댓글 리스트 불러오기
            log.info("첫 댓글 목록 조회");
            comments = this.commentRepository.findByPostIdAndDeletedAtIsNullOrderByCreatedAtDesc(postId, pageable);
        } else {
            // cursor가 존재한다면 cursor를 기반으로 다음 댓글드 불러오기
            log.info("cursor 기반 다음 댓글 조회");
            comments = this.commentRepository.findByPostIdAndIdLessThanAndDeletedAtIsNullOrderByCreatedAtDesc(postId, cursor, pageable);
        }

        boolean hasNext = comments.size() > size;
        if (hasNext) {
            comments = comments.subList(0, size);
        }

        List<CommentResponseDto> commentList = comments.stream()
                .map(comment -> CommentResponseDto.builder()
                        .id(comment.getId())
                        .author(comment.getUser().getNickname())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .isMine(comment.getUser().getId().equals(userId)) // 인증이 추가되면 로직 변경하기
                        .build())
                .toList();

        Long nextCursor = !commentList.isEmpty() ? commentList.getLast().getId() : null;

        log.info("조회된 댓글 수: {}", commentList.size());
        log.info("===== 댓글 내용 =====");
        commentList.forEach(comment ->
            log.info("댓글 ID: {}, 작성자: {}, 내용: {}, 작성일: {}, 내댓글: {}",
                    comment.getId(), comment.getAuthor(), comment.getContent(),
                    comment.getCreatedAt(), comment.isMine())
        );
        log.info("다음 커서: {}, 다음 페이지 존재: {}", nextCursor, hasNext);
        log.info("======================");

        return new CursorCommentResponseDto<>(commentList, nextCursor, hasNext);
    }

    @Transactional
    public CrudCommentResponseDto writeComment(Long postId, String token, CreateCommentRequestDto createCommentRequestDto) {
        Long userId = this.jwtUtil.extractUserIdFromToken(token);
        Post post = this.postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Not found post"));
        User user = this.userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Not found user"));

        Comment comment = new Comment();
        comment.setContent(createCommentRequestDto.getContent());
        comment.setPost(post);
        comment.setUser(user);


        Comment savedComment = this.commentRepository.save(comment);
        return new CrudCommentResponseDto(savedComment.getId());
    }

    @Transactional
    public CrudCommentResponseDto modifyComment(String token, UpdateCommentRequestDto updateCommentRequestDto) {
        Long userId = this.jwtUtil.extractUserIdFromToken(token);
        // 작성자가 맞는지부터확인
        Comment comment = this.commentRepository.findById(updateCommentRequestDto.getCommentId())
                .orElseThrow(() -> new CommentNotFoundException("Not found comment"));

        if (!userId.equals(comment.getUser().getId())) {
            throw new UnauthorizedException("You are not authorized to modify this comment");
        }

        comment.setContent(updateCommentRequestDto.getContent());
        // @Transactional에 의해 자동으로 UPDATE 쿼리 실행 (Dirty Checking)
        return new CrudCommentResponseDto(comment.getId());
    }

    @Transactional
    public CrudCommentResponseDto removeComment(Long commentId, String token) {
        Long userId = this.jwtUtil.extractUserIdFromToken(token);
        Comment comment = this.commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Not found comment"));

        if (!userId.equals(comment.getUser().getId())) {
            throw new UnauthorizedException("You are not authorized to delete this comment");
        }

        comment.setDeletedAt(java.time.LocalDateTime.now());
        // @Transactional에 의해 자동으로 UPDATE 쿼리 실행 (Dirty Checking)

        return new CrudCommentResponseDto(commentId);
    }
}
