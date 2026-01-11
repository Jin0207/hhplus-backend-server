package kr.hhplus.be.server.infrastructure.common;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {

	@CreationTimestamp
	@Column(name = "crt_dttm", nullable = false, updatable = false)
	private LocalDateTime crtDttm;

	@UpdateTimestamp
	@Column(name = "upd_dttm")
	private LocalDateTime updDttm;
}
