package com.lombard.util;

import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

// Помощник для отката транзакции когда ошибку я ловлю сам (try/catch) а не даю ей вылететь из метода.

public final class TransactionUtils {

    private TransactionUtils() {
    }

    // помечаю текущую транзакцию на откат чтобы не сохранились «половинчатые» данные
    // (деньги списались, а заказ не создался)
    public static void rollbackCurrent() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException ignored) {
            // транзакции нет (например в тестах вебслоя откатывать нечего)
        }
    }
}
