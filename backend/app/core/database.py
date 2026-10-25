from typing import AsyncGenerator
from sqlalchemy.ext.asyncio import (
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from sqlalchemy import inspect
from sqlalchemy.engine import Connection
from sqlalchemy.orm import DeclarativeBase
from app.core.config import settings

engine = create_async_engine(
    settings.DATABASE_URL,
    echo=False,
    future=True,
    connect_args={"check_same_thread": False} if "sqlite" in settings.DATABASE_URL else {},
)

AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autocommit=False,
    autoflush=False,
)


class Base(DeclarativeBase):
    pass


def _add_missing_fault_columns(connection: Connection) -> None:
    table_name = "fault_records"
    inspector = inspect(connection)
    if not inspector.has_table(table_name):
        return

    existing_columns = {column["name"] for column in inspector.get_columns(table_name)}
    missing_columns = {
        "user_title": "VARCHAR(256) NOT NULL DEFAULT ''",
        "explanation": "TEXT NOT NULL DEFAULT ''",
        "symptoms": "TEXT NOT NULL DEFAULT ''",
        "self_check_steps": "TEXT NOT NULL DEFAULT ''",
        "specialist_steps": "TEXT NOT NULL DEFAULT ''",
        "urgency": "VARCHAR(64) NOT NULL DEFAULT 'Средняя'",
        "driving_recommendation": "TEXT NOT NULL DEFAULT ''",
        "fidelity": "VARCHAR(128) NOT NULL DEFAULT 'База знаний TruckDiag'",
    }

    for column_name, column_definition in missing_columns.items():
        if column_name not in existing_columns:
            connection.exec_driver_sql(
                f"ALTER TABLE {table_name} ADD COLUMN {column_name} {column_definition}"
            )


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()


async def init_db() -> None:
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
        await conn.run_sync(_add_missing_fault_columns)
