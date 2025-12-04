package kr.hhplus.be.server.domain.user.service;

import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import kr.hhplus.be.server.domain.user.entity.User;
import kr.hhplus.be.server.domain.user.repository.UserRepository;
import kr.hhplus.be.server.support.security.service.UserDetailsCustomService;

@ExtendWith(MockitoExtension.class)
public class UserDetailsCustomServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsCustomService userDetailsCustomService;

    @DisplayName("사용자 계정 ID로 UserDetails 로딩 성공 테스트")
    @Test
    void 사용자_로딩_성공() {
        // Given
        String accountId = "testuser";
        // User.java 도메인 객체를 생성 (record 또는 class의 create/of 메서드 사용 가정)
        User mockUser = User.create(accountId, "$2a$10$encodedPassword"); 
        
        when(userRepository.findByAccountId(accountId)).thenReturn(Optional.of(mockUser));

        // When
        UserDetails userDetails = userDetailsCustomService.loadUserByUsername(accountId);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(accountId);
        assertThat(userDetails.getPassword()).isEqualTo(mockUser.password());
    }

    @DisplayName("존재하지 않는 사용자 계정 로딩 시 UsernameNotFoundException 발생 테스트")
    @Test
    void 사용자_로딩_실패() {
        // Given
        String accountId = "unknownuser";
        when(userRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, 
                    () -> userDetailsCustomService.loadUserByUsername(accountId));
    }
}