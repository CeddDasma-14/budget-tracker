package com.cedd.budgettracker.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.cedd.budgettracker.data.local.dao.BudgetSessionDao
import com.cedd.budgettracker.data.local.dao.ExpenseDao
import com.cedd.budgettracker.data.local.dao.GoalDao
import com.cedd.budgettracker.data.local.dao.TemplateDao
import com.cedd.budgettracker.data.local.entity.BudgetSessionEntity
import com.cedd.budgettracker.data.local.entity.ExpenseEntity
import com.cedd.budgettracker.data.local.entity.GoalContributionEntity
import com.cedd.budgettracker.data.local.entity.GoalEntity
import com.cedd.budgettracker.data.local.entity.TemplateEntity
import com.cedd.budgettracker.data.local.entity.TemplateExpenseEntity
import com.cedd.budgettracker.data.local.relation.BudgetSessionWithExpenses
import androidx.glance.appwidget.updateAll
import com.cedd.budgettracker.widget.BudgetWidget
import com.cedd.budgettracker.data.local.relation.GoalWithContributions
import com.cedd.budgettracker.data.local.relation.TemplateWithExpenses
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BudgetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: BudgetSessionDao,
    private val expenseDao: ExpenseDao,
    private val templateDao: TemplateDao,
    private val goalDao: GoalDao
) {

    // ── Sessions ──────────────────────────────────────────────────────────────

    fun getAllSessionsWithExpenses(): Flow<List<BudgetSessionWithExpenses>> =
        sessionDao.getAllSessionsWithExpenses()

    suspend fun getLatestSessionWithExpenses(): BudgetSessionWithExpenses? =
        sessionDao.getLatestSessionWithExpenses()

    /** Tells the home-screen widget to re-query the DB and redraw. */
    suspend fun updateWidget() = BudgetWidget().updateAll(context)

    suspend fun saveSession(session: BudgetSessionEntity): Long =
        sessionDao.insertSession(session)

    suspend fun updateSession(session: BudgetSessionEntity) =
        sessionDao.updateSession(session)

    suspend fun deleteSession(session: BudgetSessionEntity) =
        sessionDao.deleteSession(session)

    // ── Expenses ──────────────────────────────────────────────────────────────

    fun getExpensesForSession(sessionId: Long): Flow<List<ExpenseEntity>> =
        expenseDao.getExpensesForSession(sessionId)

    suspend fun saveExpenses(expenses: List<ExpenseEntity>) =
        expenseDao.insertExpenses(expenses)

    suspend fun deleteExpense(expense: ExpenseEntity) =
        expenseDao.deleteExpense(expense)

    // ── Templates ─────────────────────────────────────────────────────────────

    fun getAllTemplates(): Flow<List<TemplateWithExpenses>> =
        templateDao.getAllTemplates()

    suspend fun saveTemplate(name: String, initialBudget: Double, expenses: List<ExpenseEntity>) {
        val templateId = templateDao.insertTemplate(
            TemplateEntity(name = name, initialBudget = initialBudget)
        )
        templateDao.insertTemplateExpenses(expenses.map {
            TemplateExpenseEntity(
                templateId = templateId,
                title = it.title,
                amount = it.amount,
                category = it.category,
                isRecurring = it.isRecurring
            )
        })
    }

    suspend fun deleteTemplate(template: TemplateEntity) =
        templateDao.deleteTemplate(template)

    // ── Goals ─────────────────────────────────────────────────────────────────

    fun getAllGoals(): Flow<List<GoalWithContributions>> =
        goalDao.getAllGoalsWithContributions()

    suspend fun createGoal(goal: GoalEntity): Long =
        goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: GoalEntity) =
        goalDao.updateGoal(goal)

    suspend fun deleteGoal(goal: GoalEntity) =
        goalDao.deleteGoal(goal)

    suspend fun addContribution(contribution: GoalContributionEntity) =
        goalDao.insertContribution(contribution)

    suspend fun deleteContribution(contribution: GoalContributionEntity) =
        goalDao.deleteContribution(contribution)

    // ── Receipt image ─────────────────────────────────────────────────────────

    suspend fun saveReceiptImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "receipts").also { it.mkdirs() }
            val dest = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { input.copyTo(it) }
            }
            dest.absolutePath
        } catch (e: Exception) { null }
    }

    fun deleteReceiptImage(path: String) {
        File(path).takeIf { it.exists() }?.delete()
    }

    // ── OCR — extracts the largest monetary value from a receipt photo ────────

    suspend fun recognizeAmountFromImage(uri: Uri): String? = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text?> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            } ?: return@withContext null

            val amountRegex = Regex("""(?:₱|PHP)?\s*([\d,]+(?:\.\d{1,2})?)""")
            visionText.text.lines()
                .mapNotNull { amountRegex.find(it)?.groupValues?.get(1) }
                .maxByOrNull { it.replace(",", "").toDoubleOrNull() ?: 0.0 }
        } catch (e: Exception) { null }
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    suspend fun exportSessionToCsv(data: BudgetSessionWithExpenses): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val session = data.session
                val csv = buildString {
                    appendLine("Budget Tracker Export")
                    appendLine("Session,\"${session.name}\"")
                    appendLine("Initial Budget,${session.initialBudget}")
                    appendLine()
                    appendLine("Title,Amount,Category,Paid,Has Receipt")
                    data.expenses.forEach { e ->
                        appendLine("\"${e.title}\",${e.amount},${e.category},${e.isPaid},${e.receiptPath != null}")
                    }
                    val total = data.expenses.sumOf { it.amount }
                    appendLine()
                    appendLine("Total Expenses,$total")
                    appendLine("Remaining,${session.initialBudget - total}")
                }

                val dir  = File(context.cacheDir, "exports").also { it.mkdirs() }
                val name = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                val file = File(dir, "budget_$name.csv").also { it.writeText(csv) }

                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) { null }
        }

    fun shareCsvUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(intent, "Export Budget CSV")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
