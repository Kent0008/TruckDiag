import json
from typing import List
from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy.ext.asyncio import AsyncSession
from app.core.database import get_db
from app.schemas.diagnostic import (
    ReportCreateRequest,
    ReportDetailResponse,
    ReportResponse,
)
from app.services.diagnostic_service import (
    create_diagnostic_report,
    get_report_by_id,
    list_reports,
)

router = APIRouter()


@router.post("", response_model=ReportResponse, status_code=status.HTTP_201_CREATED)
async def post_create_report(req: ReportCreateRequest, db: AsyncSession = Depends(get_db)):
    try:
        report = await create_diagnostic_report(db, req)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))
    return ReportResponse(
        id=report.id,
        session_id=report.session_id,
        created_at=report.created_at,
        title=report.title,
        summary=report.summary,
        truck_model=report.truck_model,
        truck_vin=report.truck_vin,
        operator_name=report.operator_name,
        total_faults=report.total_faults,
        active_faults=report.active_faults,
    )


@router.get("", response_model=List[ReportResponse])
async def get_all_reports(limit: int = 50, db: AsyncSession = Depends(get_db)):
    reports = await list_reports(db, limit)
    return [
        ReportResponse(
            id=r.id,
            session_id=r.session_id,
            created_at=r.created_at,
            title=r.title,
            summary=r.summary,
            truck_model=r.truck_model,
            truck_vin=r.truck_vin,
            operator_name=r.operator_name,
            total_faults=r.total_faults,
            active_faults=r.active_faults,
        )
        for r in reports
    ]


@router.get("/{report_id}", response_model=ReportDetailResponse)
async def get_report_details(report_id: str, db: AsyncSession = Depends(get_db)):
    r = await get_report_by_id(db, report_id)
    if not r:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Report '{report_id}' not found",
        )
    return ReportDetailResponse(
        id=r.id,
        session_id=r.session_id,
        created_at=r.created_at,
        title=r.title,
        summary=r.summary,
        truck_model=r.truck_model,
        truck_vin=r.truck_vin,
        operator_name=r.operator_name,
        total_faults=r.total_faults,
        active_faults=r.active_faults,
        report_data=json.loads(r.report_data_json),
    )


@router.get("/{report_id}/export")
async def export_report_json(report_id: str, db: AsyncSession = Depends(get_db)):
    r = await get_report_by_id(db, report_id)
    if not r:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Report '{report_id}' not found",
        )
    return Response(
        content=r.report_data_json,
        media_type="application/json",
        headers={
            "Content-Disposition": f'attachment; filename="TruckDiag_Report_{r.id[:8]}.json"'
        },
    )
