package com.example.forpetapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.core.graphics.toColorInt
import com.example.forpetapp.data.model.BookingItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 📄 발자국동행 진료 리포트 PDF 생성 및 카카오톡/SNS 공유 헬퍼
 */
object ReportShareHelper {

    /**
     * 0. 갤러리에 이미지 저장 (병원 영수증/처방전 등)
     */
    fun saveBitmapToGallery(context: Context, bitmap: android.graphics.Bitmap) {
        try {
            val fileName = "발자국동행_진료자료_${System.currentTimeMillis()}.jpg"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/발자국동행")
                }
                val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    Toast.makeText(context, "📸 갤러리('발자국동행' 폴더)에 사진이 저장되었습니다.", Toast.LENGTH_LONG).show()
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "발자국동행")
                if (!appDir.exists()) appDir.mkdirs()
                val file = File(appDir, fileName)
                val outputStream = FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = android.net.Uri.fromFile(file)
                context.sendBroadcast(mediaScanIntent)
                Toast.makeText(context, "📸 갤러리('발자국동행' 폴더)에 사진이 저장되었습니다.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "사진 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 1. 카카오톡 / 메시지 텍스트 진료 리포트 공유하기
     */
    fun shareReportViaKakaoOrSms(context: Context, booking: BookingItem) {
        try {
            val shareText = """
                🐾 [발자국동행] ${booking.petName} 아이 제휴병원 진료 결과 리포트
                ────────────────────
                • 환자 정보: ${booking.petName} (${booking.species})
                • 보호자: ${booking.ownerName} 님
                • 진료 병원: ${booking.hospitalName}
                • 진료 일시: ${booking.time}
                • 이송 차량: ${booking.vehicleInfo}
                
                📋 담당 수의사 종합 소견:
                ${booking.doctorNote.ifEmpty { "진료 결과 이상 소견 없이 건강한 상태입니다." }}
                
                💊 처방 및 케어 지침:
                ${booking.prescription.ifEmpty { "별도 처방약 없음 (정상 귀가)" }}
                
                ※ 앱 내에서 병원 영수증, 수의사 진료 기록지, 처방전 등 상세 사진 자료를 확인할 수 있습니다.
                
                ────────────────────
                발자국동행 1:1 안심 케어 서비스로 안전하게 귀가 완료되었습니다.
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "[발자국동행] ${booking.petName} 진료 리포트")
            }
            context.startActivity(Intent.createChooser(intent, "진료 리포트 가족/카카오톡 공유"))
        } catch (e: Exception) {
            Toast.makeText(context, "공유하기를 실행할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 2. 정식 PDF 진료 리포트 문서 생성 및 저장
     */
    fun generateAndSavePdfReport(context: Context, booking: BookingItem): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 사이즈 (595 x 842 pt)
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = "#0284C7".toColorInt()
                textSize = 20f
                isFakeBoldText = true
            }
            val subPaint = Paint().apply {
                color = "#334155".toColorInt()
                textSize = 12f
            }
            val boldPaint = Paint().apply {
                color = "#0F172A".toColorInt()
                textSize = 13f
                isFakeBoldText = true
            }
            val linePaint = Paint().apply {
                color = "#CBD5E1".toColorInt()
                strokeWidth = 1.5f
            }

            // 상단 헤더
            canvas.drawText("🐾 발자국동행 수의사 정식 진료 리포트", 40f, 60f, titlePaint)
            canvas.drawText("발행처: (주)발자국동행 반려동물 안심병원 동행센터", 40f, 85f, subPaint)
            val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN).format(Date())
            canvas.drawText("발행일시: $nowStr", 380f, 85f, subPaint)
            canvas.drawLine(40f, 100f, 555f, 100f, linePaint)

            // 기본 정보 박스
            var y = 130f
            canvas.drawText("1. 기본 진료 및 이송 개요", 40f, y, boldPaint); y += 24f
            canvas.drawText("• 환자명: ${booking.petName} (${booking.species})", 50f, y, subPaint); y += 20f
            canvas.drawText("• 보호자 성함: ${booking.ownerName}", 50f, y, subPaint); y += 20f
            canvas.drawText("• 진료 동물병원: ${booking.hospitalName}", 50f, y, subPaint); y += 20f
            canvas.drawText("• 안심 이송 차량: ${booking.vehicleInfo}", 50f, y, subPaint); y += 20f
            canvas.drawText("• 담당 가드: 박찬헌 가드 (1:1 안심 모니터링 수행)", 50f, y, subPaint); y += 30f

            canvas.drawLine(40f, y, 555f, y, linePaint); y += 25f

            // 2. 수의사 소견
            canvas.drawText("2. 담당 수의사 종합 소견", 40f, y, boldPaint); y += 24f
            val note = booking.doctorNote.ifEmpty { "진료 결과 특이 이상 소견 없이 건강한 상태이며 기본 활력 징후 양호합니다." }
            val words = note.chunked(38)
            for (w in words) {
                canvas.drawText(w, 50f, y, subPaint)
                y += 20f
            }
            y += 15f

            canvas.drawLine(40f, y, 555f, y, linePaint); y += 25f

            // 3. 처방 및 홈케어 지침
            canvas.drawText("3. 처방 및 홈케어 안내", 40f, y, boldPaint); y += 24f
            val prescription = booking.prescription.ifEmpty { "별도 내복약 처방 없음. 스트레스 완화 및 충분한 수분 섭취 권장." }
            val pWords = prescription.chunked(38)
            for (w in pWords) {
                canvas.drawText(w, 50f, y, subPaint)
                y += 20f
            }
            y += 30f

            canvas.drawLine(40f, y, 555f, y, linePaint); y += 30f

            // 4. 첨부 파일 안내
            val hasPhotos = !booking.receiptImageUri.isNullOrBlank() || !booking.clinicalNoteImageUri.isNullOrBlank() || !booking.prescriptionImageUri.isNullOrBlank()
            if (hasPhotos) {
                canvas.drawText("4. 영수증 및 진료 자료 첨부", 40f, y, boldPaint); y += 24f
                canvas.drawText("앱 내 진료 리포트에서 병원 영수증, 소견서, 처방전을 사진으로 확인 가능합니다.", 50f, y, subPaint); y += 20f
                canvas.drawLine(40f, y, 555f, y, linePaint); y += 30f
            }

            // 5. 하단 직인 및 보증
            canvas.drawText("상기 진료 대행 및 안심 케어 과정이 이상 없이 완료되었음을 공식 확인합니다.", 50f, y, subPaint); y += 30f
            canvas.drawText("발자국동행 동물병원 동행 케어 총괄", 260f, y, boldPaint)
            val sealPaint = Paint().apply {
                color = "#DC2626".toColorInt()
                textSize = 14f
                isFakeBoldText = true
            }
            canvas.drawText(" [동행인증 ㊞]", 450f, y, sealPaint)

            pdfDoc.finishPage(page)

            val fileName = "발자국동행_진료리포트_${booking.petName}_${System.currentTimeMillis()}.pdf"

            // 스마트폰 '다운로드' 폴더에 저장 (MediaStore 활용)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/발자국동행")
                }

                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDoc.writeTo(outputStream)
                    }
                    pdfDoc.close()
                    Toast.makeText(context, "📄 내 파일 > 다운로드 > '발자국동행' 폴더에 PDF가 저장되었습니다.", Toast.LENGTH_LONG).show()
                    return null // 안드로이드 10 이상은 File 객체 대신 Uri를 사용하지만 리턴은 생략
                }
            } else {
                // 안드로이드 9 이하 레거시 저장
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "발자국동행")
                if (!appDir.exists()) appDir.mkdirs()

                val file = File(appDir, fileName)
                val outputStream = FileOutputStream(file)
                pdfDoc.writeTo(outputStream)
                outputStream.close()
                pdfDoc.close()

                Toast.makeText(context, "📄 다운로드 폴더에 PDF가 저장되었습니다.", Toast.LENGTH_LONG).show()
                return file
            }
            
            pdfDoc.close()
            null
        } catch (e: Exception) {
            Toast.makeText(context, "PDF 저장 실패 (저장소 권한 등을 확인하세요): ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
