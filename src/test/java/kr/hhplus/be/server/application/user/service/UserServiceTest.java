package kr.hhplus.be.server.application.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.support.exception.BusinessException;
import kr.hhplus.be.server.support.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    @DisplayName("성공: 사용자 조회를 성공한다")
    void 사용자_조회_성공() {
        // given
        Long userId = 1L;
        User user = new User(
            1L, "test@test.com", "test1234", 10000L,
            LocalDateTime.now(), null
        );
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // when
        User result = userService.getUser(userId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.password()).isEqualTo("test1234");
        assertThat(result.accountId()).isEqualTo("test@test.com");
    }
    
    @Test
    @DisplayName("실패: 존재하지 않는 사용자 조회 시 예외가 발생한다")
    void 존재하지_않는_사용자_조회_예외() {
        // given
        Long userId = 999L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> userService.getUser(userId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
    
    @Test
    @DisplayName("성공: 사용자를 저장한다.")
    void 사용자_저장_성공() {
        // given
        User user = new User(
            null, "new@test.com", "test1234", 0L,
            LocalDateTime.now(), null
        );
        
        User savedUser = new User(
            1L, "new@test.com", "test1234", 0L,
            LocalDateTime.now(), null
        );
        
        when(userRepository.save(user)).thenReturn(savedUser);
        
        // when
        User result = userService.save(user);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.accountId()).isEqualTo("new@test.com");
        
        verify(userRepository).save(user);
    }
    
    @Test
    @DisplayName("성공: 아이디로 사용자를 조회한다.")
    void 아이디로_사용자_조회_성공() {
        // given
        String accountId = "test@test.com";
        User user = new User(
            1L, accountId, "테스터", 10000L,
            LocalDateTime.now(), null
        );
        
        when(userRepository.findByAccountId(accountId)).thenReturn(Optional.of(user));
        
        // when
        User result = userService.getUserByAccountId(accountId);
        
        // then
        assertThat(result.accountId()).isEqualTo(accountId);
    }
    
    @Test
    @DisplayName("실패: 아이디로 사용자 조회 시 없으면 에러를 반환한다")
    void 아이디로_사용자_조회_없음() {
        // given
        String accountId = "test@test.com";
        
        // when & then
        assertThatThrownBy(() -> userService.getUserByAccountId(accountId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        
        verify(userRepository, never()).save(any());
    }
        
    @Test
    @DisplayName("실패: 이미 존재하는 아이디로 회원가입 시 예외가 발생한다")
    void 아이디_중복_생성_예외() {
        // given
        String accountId = "test@test.com";
        String password = "password123";
        
        User existingUser = new User(
            1L, accountId, password, 0L,
            LocalDateTime.now(), null
        );
        
        when(userRepository.existsByAccountId(accountId))
                .thenReturn(true);        
        // when & then
        assertThatThrownBy(() -> userService.createUser(existingUser))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);
        
        verify(userRepository, never()).save(any());
    }
    
}