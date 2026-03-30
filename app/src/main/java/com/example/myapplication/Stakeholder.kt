import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Stakeholder(
    val id: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val type: String? = null, // اجعلها اختيارية بوضع ? وحرف الـ = null

    @SerialName("initial_balance")
    val initialBalance: Double? = 0.0,

    @SerialName("company_id")
    val companyId: String? = null,

    @SerialName("calculated_balance")
    val currentBalance: Double? = null
)