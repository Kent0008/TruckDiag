from sqlalchemy import create_engine, inspect

from app.core.database import _add_missing_fault_columns


def test_legacy_fault_table_gets_diagnostic_columns():
    engine = create_engine("sqlite:///:memory:")
    with engine.begin() as connection:
        connection.exec_driver_sql(
            "CREATE TABLE fault_records (id VARCHAR(64) PRIMARY KEY, code VARCHAR(64))"
        )
        connection.exec_driver_sql(
            "INSERT INTO fault_records (id, code) VALUES ('fault-1', 'SPN 110 FMI 0')"
        )

        _add_missing_fault_columns(connection)

        columns = {column["name"] for column in inspect(connection).get_columns("fault_records")}
        defaults = connection.exec_driver_sql(
            "SELECT user_title, urgency, fidelity FROM fault_records"
        ).one()

    assert {
        "user_title",
        "explanation",
        "symptoms",
        "self_check_steps",
        "specialist_steps",
        "urgency",
        "driving_recommendation",
        "fidelity",
    }.issubset(columns)
    assert defaults == ("", "Средняя", "База знаний TruckDiag")