package org.example.collectfocep.collectfocep.services.impl;

import org.example.collectfocep.services.impl.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TransactionStatus transactionStatus;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        // Injecter manuellement le mock dans le service
        ReflectionTestUtils.setField(transactionService, "transactionTemplate", transactionTemplate);

        // Configurer le comportement du mock
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });

        // Pour éviter l'erreur sur setPropagationBehavior
        doNothing().when(transactionTemplate).setPropagationBehavior(anyInt());
    }

    @Test
    void testExecuteInTransaction_Success() {
        // Arrange
        String expectedResult = "Transaction successful";
        TransactionCallback<String> action = status -> expectedResult;

        // Act
        String result = transactionService.executeInTransaction(action);

        // Assert
        assertEquals(expectedResult, result);
    }

    @Test
    void testExecuteInTransaction_Exception() {
        // Arrange
        RuntimeException exception = new RuntimeException("Transaction failed");
        TransactionCallback<String> action = status -> {
            throw exception;
        };

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            transactionService.executeInTransaction(action);
        });
    }

    @Test
    void testExecuteInNewTransaction_Success() {
        // Arrange
        String expectedResult = "New transaction successful";
        TransactionCallback<String> action = status -> expectedResult;

        // Mock additional behavior for new transaction
        when(transactionTemplate.getPropagationBehavior()).thenReturn(0); // Initial value
        doNothing().when(transactionTemplate).setPropagationBehavior(anyInt());

        // Act
        String result = transactionService.executeInNewTransaction(action);

        // Assert
        assertEquals(expectedResult, result);
        verify(transactionTemplate).setPropagationBehavior(anyInt());
    }

    @Test
    void testExecuteInNewTransaction_Exception() {
        // Arrange
        RuntimeException exception = new RuntimeException("New transaction failed");
        TransactionCallback<String> action = status -> {
            throw exception;
        };

        // Mock additional behavior for new transaction
        when(transactionTemplate.getPropagationBehavior()).thenReturn(0); // Initial value
        doNothing().when(transactionTemplate).setPropagationBehavior(anyInt());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            transactionService.executeInNewTransaction(action);
        });
        verify(transactionTemplate).setPropagationBehavior(anyInt());
    }
}
