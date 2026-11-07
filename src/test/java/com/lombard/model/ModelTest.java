package com.lombard.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//Тесты остальных сущностей: счет, операции по счету, клиент, залог, уведомления, история займа и т.д. Проверка методов которые вызываются перед сохранением в базу (@PrePersist/@PreUpdate) и вспомогательные геттеры

class ModelTest {

    @Test
    void account_prePersistAndPreUpdate_setsDatesAndZeroBalance() {
        Account a = new Account();
        a.setBalance(null);

        a.prePersist();
        assertNotNull(a.getCreatedAt());
        assertEquals(BigDecimal.ZERO, a.getBalance());

        a.preUpdate();
        assertNotNull(a.getUpdatedAt());
    }

    @Test
    void accountTransaction_prePersist_setsDate() {
        AccountTransaction t = new AccountTransaction();

        t.prePersist();

        assertNotNull(t.getCreatedAt());
    }

    // тип операции показывается на странице счета (русский язык)
    @Test
    void accountTransaction_typeLabel_returnsRussianText() {
        AccountTransaction t = new AccountTransaction();
        t.setType("DEPOSIT");
        assertEquals("Пополнение", t.getTypeLabel());
        t.setType("WITHDRAW");
        assertEquals("Вывод", t.getTypeLabel());
        t.setType("PURCHASE");
        assertEquals("Покупка", t.getTypeLabel());
        t.setType("REFUND");
        assertEquals("Возврат", t.getTypeLabel());
        t.setType("OTHER");
        assertEquals("OTHER", t.getTypeLabel());
    }

    @Test
    void cartItem_prePersist_setsAddedDate() {
        CartItem ci = new CartItem();

        ci.prePersist();

        assertNotNull(ci.getAddedAt());
    }

    @Test
    void notification_prePersist_setsDateAndUnread() {
        Notification n = new Notification();

        n.prePersist();

        assertNotNull(n.getCreatedAt());
        assertFalse(n.getRead());
    }

    // в уведомление можно передать до 5 параметров для текста
    @Test
    void notification_constructorWithArgs_savesArgs() {
        User u = new User("u", "p", "CLIENT");

        Notification n = new Notification(u, "EVT", "INFO", "a", "b", "c", "d", "e");

        assertEquals("a", n.getArg1());
        assertEquals("e", n.getArg5());
    }

    // если номер не успели выдать ставится заглушка *_UNKNOWN
    @Test
    void userAndCollateral_prePersist_setsTemporaryNumbers() {
        User u = new User();
        u.prePersist();
        assertEquals("U_UNKNOWN", u.getUserNumber());

        Collateral c = new Collateral();
        c.prePersist();
        assertEquals("M_UNKNOWN", c.getItemNumber());
    }

    // фио собирается из фамилии имени и отчества
    @Test
    void client_rebuildFullName_joinsNameParts() {
        Client c = new Client();
        c.setLastName("Иванов");
        c.setFirstName("Иван");
        c.setMiddleName("Иванович");

        c.rebuildFullName();

        assertEquals("Иванов Иван Иванович", c.getFullName());
        assertEquals("C_UNKNOWN", c.getClientNumber());
    }

    @Test
    void client_rebuildFullNameWithoutParts_setsNoName() {
        Client c = new Client();

        c.rebuildFullName();

        assertEquals("Без имени", c.getFullName());
    }

    // главное фото залога - первое из списка
    @Test
    void collateral_photos_returnsMainPhoto() {
        Collateral col = new Collateral();
        col.setPhotoUrls(new ArrayList<>());
        assertFalse(col.hasPhotos());
        assertNull(col.getMainPhoto());

        col.getPhotoUrls().add("/uploads/a.jpg");
        assertTrue(col.hasPhotos());
        assertEquals("/uploads/a.jpg", col.getMainPhoto());
    }

    @Test
    void numberSequence_gettersAndSetters_work() {
        NumberSequence ns = new NumberSequence();
        ns.setPrefix("L");
        ns.setNextValue(5L);

        assertEquals("L", ns.getPrefix());
        assertEquals(5L, ns.getNextValue());
    }

    @Test
    void loanHistory_constructor_setsActionAndDate() {
        LoanHistory h = new LoanHistory(new Loan(), "action", "admin");

        assertEquals("action", h.getAction());
        assertNotNull(h.getChangedAt());
    }

    // админ вводит ФИО одной строкой - она раскладывается на фамилию, имя и отчество
    @Test
    void client_changeFullName_splitsIntoParts() {
        Client c = new Client();
        c.setLastName("Старый");

        c.changeFullName("  Петров   Пётр Сергеевич ");
        c.rebuildFullName(); // то же самое, что вызовется перед сохранением

        assertEquals("Петров Пётр Сергеевич", c.getFullName());
        assertEquals("Петров", c.getLastName());
        assertEquals("Пётр", c.getFirstName());
        assertEquals("Сергеевич", c.getMiddleName());
    }
}
