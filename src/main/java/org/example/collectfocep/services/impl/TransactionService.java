package org.example.collectfocep.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class TransactionService {

    private final TransactionTemplate transactionTemplate;

    @Autowired 
    public TransactionService(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public <T> T executeInTransaction(TransactionCallback<T> action) {
        try {
            return transactionTemplate.execute(action);
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de la transaction", e);
            throw e;
        }
    }

    public <T> T executeInNewTransaction(TransactionCallback<T> action) {
        int originalPropagation = transactionTemplate.getPropagationBehavior();
        try {
            // Configurer pour une nouvelle transaction
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return transactionTemplate.execute(action);
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution de la nouvelle transaction", e);
            throw e;
        } finally {
            // Restaurer la propagation d'origine
            transactionTemplate.setPropagationBehavior(originalPropagation);
        }
    }
}