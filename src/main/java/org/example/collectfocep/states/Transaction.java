package org.example.collectfocep.states;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Transient
    private TransactionState state;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Ces méthodes appellent l'état actuel pour effectuer l'opération
    public void validate() {
        state.validate(this);
    }

    public void process() {
        state.process(this);
    }

    public void complete() {
        state.complete(this);
    }

    public void fail() {
        state.fail(this);
    }

    public void cancel() {
        state.cancel(this);
    }

    // Méthodes de mise à jour d'état
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public void setState(TransactionState state) {
        this.state = state;
    }
}