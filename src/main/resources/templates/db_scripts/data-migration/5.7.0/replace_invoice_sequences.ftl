<#if mode.name() == "UPDATE">

DO $$
DECLARE
    seq            record;
    seqName        varchar;
    invoiceId      uuid;
    lastValue      int;
    seqInitialized boolean;
BEGIN
    FOR seq IN
        SELECT sequence_name
            FROM information_schema.sequences
            WHERE sequence_schema = '${myuniversity}_${mymodule}' AND sequence_name LIKE 'ilNumber_%'
    LOOP
        seqName := seq.sequence_name;
        invoiceId := substring(seqName from 10)::uuid;
        EXECUTE 'SELECT is_called FROM ' || quote_ident(seqName) INTO seqInitialized;
        IF seqInitialized THEN
            EXECUTE 'SELECT last_value FROM ' || quote_ident(seqName) INTO lastValue;
        ELSE
            lastValue := 0;
        END IF;
        UPDATE ${myuniversity}_${mymodule}.invoices
            SET jsonb = jsonb || jsonb_build_object('nextInvoiceLineNumber', lastValue+1)
            WHERE id=invoiceId AND NOT jsonb ? 'nextInvoiceLineNumber';
        EXECUTE 'DROP SEQUENCE ' || quote_ident(seqName);
    END LOOP;
END $$;

</#if>
