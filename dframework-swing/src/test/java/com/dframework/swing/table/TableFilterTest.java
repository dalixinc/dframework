package com.dframework.swing.table;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class TableFilterTest {

    private static final List<String> COLUMNS = List.of("Call", "Customer", "Summary", "Status");

    @Test
    void parsesWordsPhrasesNegationAndColumns() {
        assertThat(TableFilter.parse("Acme \"printer fault\" -closed status:open", COLUMNS)).containsExactly(
                new TableFilter.Term(-1, "acme", false),
                new TableFilter.Term(-1, "printer fault", false),
                new TableFilter.Term(-1, "closed", true),
                new TableFilter.Term(3, "open", false));
    }

    @Test
    void columnPrefixesResolveByStartOfName() {
        assertThat(TableFilter.parse("cust:acme", COLUMNS)).containsExactly(new TableFilter.Term(1, "acme", false));
        assertThat(TableFilter.parse("-stat:\"on hold\"", COLUMNS)).containsExactly(new TableFilter.Term(3, "on hold", true));
    }

    @Test
    void unknownPrefixIsSearchedAsText() {
        assertThat(TableFilter.parse("10:30", COLUMNS)).containsExactly(new TableFilter.Term(-1, "10:30", false));
    }

    @Test
    void unterminatedQuoteTakesTheRest() {
        assertThat(TableFilter.parse("\"printer fa", COLUMNS)).containsExactly(new TableFilter.Term(-1, "printer fa", false));
    }

    @Test
    void matchingRequiresAllTermsAndHonoursNegation() {
        String[] row = {"1042", "Acme Ltd", "Printer fault on floor 2", "Open"};
        int n = row.length;

        assertThat(TableFilter.matches(TableFilter.parse("acme printer", COLUMNS), c -> row[c], n)).isTrue();
        assertThat(TableFilter.matches(TableFilter.parse("acme scanner", COLUMNS), c -> row[c], n)).isFalse();
        assertThat(TableFilter.matches(TableFilter.parse("-open", COLUMNS), c -> row[c], n)).isFalse();
        assertThat(TableFilter.matches(TableFilter.parse("status:open", COLUMNS), c -> row[c], n)).isTrue();
        assertThat(TableFilter.matches(TableFilter.parse("status:acme", COLUMNS), c -> row[c], n)).isFalse();
    }
}
