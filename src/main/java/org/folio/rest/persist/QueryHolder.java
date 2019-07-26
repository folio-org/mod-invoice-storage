package org.folio.rest.persist;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.cql.CQLWrapper;

import java.util.LinkedList;
import java.util.List;

import static org.folio.rest.impl.VoucherStorageImpl.ACQUISITIONS_UNIT_ASSIGNMENTS;

public class QueryHolder {

  public static final String JSONB = "jsonb";
  public static final String GROUP_JSONB = "group_jsonb";

  private String table;
	private String query;
	private int offset;
	private int limit;
	private String lang;

  public QueryHolder(String table, String query, int offset, int limit, String lang) {
    this.table = table;
    this.query = query;
    this.offset = offset;
    this.limit = limit;
    this.lang = lang;
  }


	public String getTable() {
		return table;
	}

	public String getQuery() {
		return query;
	}

	public int getLimit() {
		return limit;
	}

	public String getLang() {
		return lang;
	}

  public CQLWrapper buildCQLQuery() throws FieldException {
    if (query != null && query.contains(ACQUISITIONS_UNIT_ASSIGNMENTS)) {
      query = convertQuery(query, table);
      List<String> fields = new LinkedList<>();
      fields.add(table + "." + JSONB);
      fields.add(table + "." + GROUP_JSONB);
      CQL2PgJSON cql2pgJson = new CQL2PgJSON(fields);
      return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    } else {
      CQL2PgJSON cql2pgJson = new CQL2PgJSON(table + "." + JSONB);
      return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit)).setOffset(new Offset(offset));
    }
  }

  private String convertQuery(String cql, String view){
    return cql.replaceAll("(?i)acquisitionsUnitAssignments\\.", view + "." + GROUP_JSONB + ".");
  }

}
