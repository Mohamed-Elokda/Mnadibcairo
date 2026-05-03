package com.example.myapplication.data

import com.example.myapplication.data.local.entity.Customer
import com.example.myapplication.data.local.entity.InboundDetailesEntity
import com.example.myapplication.data.local.entity.InboundEntity
import com.example.myapplication.data.local.entity.InboundWithSupplier
import com.example.myapplication.data.local.entity.ItemsEntity
import com.example.myapplication.data.local.entity.OutboundDetailesEntity
import com.example.myapplication.data.local.entity.OutboundEntity
import com.example.myapplication.data.local.entity.OutboundWithCustomer
import com.example.myapplication.data.local.entity.PaymentEntity
import com.example.myapplication.data.local.entity.ReturnedDetailsEntity
import com.example.myapplication.data.local.entity.ReturnedEntity
import com.example.myapplication.data.local.entity.ReturnedWithNames
import com.example.myapplication.data.local.entity.StockEntity
import com.example.myapplication.data.local.entity.StockWithItemName
import com.example.myapplication.data.local.entity.Supplied
import com.example.myapplication.data.local.entity.TransferDetailsEntity
import com.example.myapplication.data.local.entity.TransferEntity
import com.example.myapplication.data.local.entity.VaultEntity
import com.example.myapplication.data.remote.dto.CustomerDto
import com.example.myapplication.data.remote.dto.InboundDetailsDto
import com.example.myapplication.data.remote.dto.InboundDto
import com.example.myapplication.data.remote.dto.ItemsDto
import com.example.myapplication.data.remote.dto.OutboundDetailsDto
import com.example.myapplication.data.remote.dto.OutboundDto
import com.example.myapplication.data.remote.dto.PaymentDto
import com.example.myapplication.data.remote.dto.ReturnedDetailsDto
import com.example.myapplication.data.remote.dto.ReturnedDto
import com.example.myapplication.data.remote.dto.StockDto
import com.example.myapplication.data.remote.dto.TransferDetailDto
import com.example.myapplication.data.remote.dto.TransferDto
import com.example.myapplication.domin.model.CustomerModel
import com.example.myapplication.domin.model.Inbound
import com.example.myapplication.domin.model.InboundDetails
import com.example.myapplication.domin.model.Items
import com.example.myapplication.domin.model.Outbound
import com.example.myapplication.domin.model.OutboundDetails
import com.example.myapplication.domin.model.PaymentItem
import com.example.myapplication.domin.model.PaymentMethod
import com.example.myapplication.domin.model.ReturnedDetailsModel
import com.example.myapplication.domin.model.ReturnedModel
import com.example.myapplication.domin.model.ReturnedWithNameModel
import com.example.myapplication.domin.model.Stock
import com.example.myapplication.domin.model.SuppliedModel
import com.example.myapplication.domin.model.Transfer
import com.example.myapplication.domin.model.TransferDetails
import com.example.myapplication.domin.model.Vault
import com.example.myapplication.domin.model.VaultOperationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- تحويل Inbound ---
fun InboundWithSupplier.toDomain(): Inbound {
    return Inbound(
        id = this.id,
        userId = this.userId,
        image = this.image,
        inboundDate = this.inboundDate.formatToEnglish(),
        latitude = this.latitude,
        longitude = this.longitude,
        isSynced = this.isSynced,
        invorseNum = this.invorseNum,
        fromSppliedId = this.fromSppliedId,
        suppliedName = this.suppliedName,
    )
}
fun VaultEntity.toDomain(): Vault {
    return Vault(
        id = id,
        amount = amount,
        operationType = if (operation_type == "collection") VaultOperationType.COLLECTION else VaultOperationType.DEPOSIT,
        paymentMethod = if (payment_method == "cash") PaymentMethod.CASH else PaymentMethod.E_WALLET,
        referenceId = reference_id,
        notes = notes,
        createdAt = created_at,
        updatedAt = updated_at
    )
}

fun Vault.toEntity(isSynced: Boolean = false): VaultEntity {
    return VaultEntity(
        id = id,
        amount = amount,
        operation_type = if (operationType == VaultOperationType.COLLECTION) "collection" else "deposit",
        payment_method = if (paymentMethod == PaymentMethod.CASH) "cash" else "e-wallet",
        reference_id = referenceId,
        notes = notes,
        created_at = createdAt,
        updated_at = updatedAt,
    )
}
fun Long.toIsoString(): String {
    val date = Date(this)
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
    return format.format(date)
}
// في ملف PaymentEntity.kt (خارج الكلاس) أو في ملف منفصل
fun PaymentEntity.toDomainModel(customerName: String): PaymentItem {
    return PaymentItem(
        id = this.id,
        customerName = customerName,
        amount = this.amount,
        date = this.date,
        paymentType = this.paymentType,
        userId = this.id,
        notes = this.notes // أو قم بتمريره حسب حاجتك
    )
}


// من Entity (موبايل) إلى Dto (سيرفر)
fun PaymentEntity.toDto() = PaymentDto(
    id = id,
    customer_id = customerId,
    amount = amount,
    payment_type = paymentType,
    date = date,
    notes = notes,
    updated_at = updatedAt,
    user_id = ""
)

fun ItemsDto.toEntity(): ItemsEntity {
    return ItemsEntity(
        // ملاحظة: إذا كان الـ Primary Key في الموبايل هو الـ itemNum
        // تأكد من تمريره بشكل صحيح هنا
        itemNum = this.itemNum.toInt(),
        itemName = this.itemName,

        updatedAt = this.updated_at ?: System.currentTimeMillis()
    )
}

// من Dto (سيرفر) إلى Entity (موبايل)
fun PaymentDto.toEntity() = PaymentEntity(
    id = id,
    customerId = customer_id,
    amount = amount,
    paymentType = payment_type,
    date = date,
    notes = notes,
    updatedAt = updated_at
)
fun OutboundDetailesEntity.toDto(): OutboundDetailsDto {
    return OutboundDetailsDto(

        outbound_id = this.outboundId,
        item_id = this.itemId,
        amount = this.amount.toDouble(),
        updated_at = this.updatedAt,
        price = this.price,
        id = this.id,
    )
}


fun ReturnedModel.toReturnedEntity(): ReturnedEntity{
    return ReturnedEntity(
        id = this.id,
        customerId = this.customerId,
        returnedDate = returnedDate.formatToEnglish(),
        latitude = this.latitude,
        longitude = this.longitude,
        invoiceNum=this.invoiceNum,
        isSynced = false,
        userId = this.userId
    )


}
  fun ReturnedEntity.toReturnedModel(): ReturnedModel{
    return ReturnedModel(
        id = this.id,
        customerId = this.customerId,
        returnedDate = returnedDate.toString().formatToEnglish(),
        latitude = this.latitude,
        longitude = this.longitude,
        userId = this.userId,
        updateAt = this.updatedAt,
        invoiceNum = this.invoiceNum
    )


}
fun ReturnedWithNames.toReturnedModel(): ReturnedWithNameModel {
    return ReturnedWithNameModel(
        returnedModel = ReturnedModel(
            id = this.returned.id,
            customerId = this.returned.customerId,
            returnedDate = this.returned.returnedDate.toString().formatToEnglish(),
            latitude = this.returned.latitude,
            longitude = this.returned.longitude,
            userId = this.returned.userId,
            updateAt = this.returned.updatedAt,
            invoiceNum = this.returned.invoiceNum
        ),
        customerName = this.customerName?:"",
        itemName = this.itemName?:"",
        totalPrice = 1.1
    )


}

fun ReturnedDetailsEntity.toReturnedDetailsModel(): ReturnedDetailsModel{
    return ReturnedDetailsModel(
        id = this.id,
        returnedId = this.returnedId,
        itemId = this.itemId,
        amount = this.amount,
        price = this.price,
        itemName = ""
    )
}
fun ReturnedDetailsModel.toReturnedEntity(): ReturnedDetailsEntity{
    return ReturnedDetailsEntity(
        id = this.id,
        returnedId = this.returnedId,
        itemId = this.itemId,
        amount = this.amount,
        price = this.price
    )
}
    // دالة التحويل
    fun OutboundWithCustomer.toDomain(): Outbound {
        return Outbound(
            id = outbound.id,
            userId = outbound.userId,
            customerId = outbound.customerId,
            customerName = customerName ?: "", // نمرر الاسم المجلوب من الـ JOIN
            invorseNumber = outbound.invorseNumber,
            image = outbound.image,
            previousDebt = outbound.previousDebt,
            totalRemainder = outbound.totalRemainder,
            outboundDate = outbound.outboundDate.toString().formatToEnglish(),
            moneyResive = outbound.moneyResive,
            isSynced = outbound.isSynced,
            latitude = outbound.latitude,
            longitude = outbound.longitude,
            updatedAt = outbound.updatedAt
        )
    }
// في ملف Mappers.kt
// في ملف Mappers.kt
fun ReturnedDto.toEntity(): ReturnedEntity {
    return ReturnedEntity(
        id = this.id, // أو حسب تعريف الـ ID عندك
        userId = this.user_id,
        customerId = this.customer_id,
        isSynced = true,
        returnedDate = this.returned_date,
        latitude = this.latitude,
        longitude = this.longitude,
        invoiceNum = this.invoiceNum,
        updatedAt = this.updated_at
    )
}
// تحويل بيانات السيرفر إلى جدول المخزن المحلي
fun StockDto.toEntity(): StockEntity {
    return StockEntity(
        id = this.id ?: 0,
        ItemId = this.itemId,
        userId = this.userId, // تأكد أن الحقل في Entity هو String أو Int حسب تصميمك
        CurrentAmount = this.currentAmount.toInt(),
        InitAmount=this.initAmount.toInt(),
        isSynced = true // القادم من السيرفر يعتبر مزامناً
    )
}

fun Inbound.toEntity(): InboundEntity {
    return InboundEntity(
        id = this.id,
        userId = this.userId,
        image = this.image,
        inboundDate = this.inboundDate.toString().formatToEnglish(),
        latitude = this.latitude,
        longitude = this.longitude,
        isSynced = this.isSynced,
        invorseNum = this.invorseNum,
        fromSppliedId = this.fromSppliedId,
    )
}
fun Outbound.toEntity(): OutboundEntity {
    return OutboundEntity(
        id = this.id,
        userId = this.userId, // تحويل Int إلى String إذا كان الـ Entity يتوقع String
        customerId = this.customerId,
        image = this.image,
        outboundDate = this.outboundDate.formatToEnglish(),
        latitude = this.latitude,
        longitude = this.longitude,
        moneyResive = this.moneyResive,
        isSynced = this.isSynced,
        invorseNumber = this.invorseNumber,
        previousDebt = this.previousDebt,
        totalRemainder = this.totalRemainder,
        updatedAt =this.updatedAt
        // تأكد من إضافة invorseNumber في الـ OutboundEntity أيضاً إذا لم تكن موجودة
    )
}
fun OutboundDto.toEntity(): OutboundEntity {
    return OutboundEntity(
        id = this.id ,
        userId = this.userId, // تحويل Int إلى String إذا كان الـ Entity يتوقع String
        customerId = this.customerId,
        image = this.imageUrl,
        outboundDate = this.outboundDate.toString().formatToEnglish(),
        latitude = this.latitude,
        longitude = this.longitude,
        moneyResive = this.moneyReceive,
        isSynced = true,
        invorseNumber = this.invoiceNumber,
        previousDebt = this.previousDebt?:0.0,
        totalRemainder = this.totalRemainder?:0.0,
        // تأكد من إضافة invorseNumber في الـ OutboundEntity أيضاً إذا لم تكن موجودة
    )
}
// تحويل من Entity (قاعدة البيانات) إلى Domain (الواجهة)
fun Customer.toDomain(): CustomerModel {
    return CustomerModel(
        id = this.id,
        userId = this.userId,
        customerName = this.customerName,
        customerNum = this.customerNum,
        customerDebt = this.customerDebt,
        firstCustomerDebt = this.firstCustomerDebt,
        updatedAt = this.updatedAt?:0,
    )
}


// تحويل من Customer Entity إلى Customer Domain Model


// تحويل من Outbound Domain Model إلى Entity (للحفظ في قاعدة البيانات)


// في ملف OutboundDetails.kt (الدومين)
fun OutboundDetails.toEntity(outboundId: String): OutboundDetailesEntity {
    return OutboundDetailesEntity(
        id = this.id,
        outboundId = outboundId,
        itemId = this.itemId,
        amount = this.amount,
        price = this.price,
        isSynced = this.isSynced
    )
}
fun OutboundDetailsDto.toEntity(outboundId: String): OutboundDetailesEntity {
    return OutboundDetailesEntity(
        id = this.id,
        outboundId = outboundId,
        itemId = this.item_id,
        amount = this.amount.toInt(),
        price = this.price,
        isSynced = true
    )
}
// --- تحويل InboundDetails ---
fun InboundDetails.toEntity(parentInboundId: String): InboundDetailesEntity {
    return InboundDetailesEntity(
        id = this.id,
        InboundId = parentInboundId,
        ItemId = this.ItemId,
        amount = this.amount
    )
}
fun Customer.toDto() = CustomerDto(
    id = this.id,
    user_id = this.userId,
    customer_name = this.customerName,
    customer_num = this.customerNum,
    customer_debt = this.customerDebt,
    is_sync = this.isSync,
    firstCustomerDebt = this.firstCustomerDebt,
)
 fun String.formatToEnglish(): String {
    val arabic = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
    var result = this
    for (i in 0..9) {
        result = result.replace(arabic[i], i.toString())
    }
    return result
}
fun OutboundEntity.toDto(): OutboundDto {
    return OutboundDto(
        userId = this.userId,
        customerId = this.customerId,
        invoiceNumber = this.invorseNumber, // لاحظ لو فيه اختلاف في الحروف (invorse vs invoice)
        imageUrl = this.image ?: "",
        outboundDate = this.outboundDate.formatToEnglish(),
        latitude = this.latitude,
        longitude = this.longitude,

        moneyReceive = this.moneyResive,
        previousDebt = this.previousDebt,
        totalRemainder = this.totalRemainder,
        updated_at = this.updatedAt,
        id = this.id,
    )
}

// مثال للدالة اللي هتحتاجها في الـ Repository
fun ReturnedEntity.toDomain() = ReturnedModel(
    id = this.id,
    customerId = this.customerId,
    returnedDate = this.returnedDate,
    userId = this.userId,
    latitude = this.latitude,
    longitude = this.longitude,
    updateAt = this.updatedAt,
    invoiceNum = this.invoiceNum,
)

fun ReturnedModel.toEntity(isSynced: Boolean = false) = ReturnedEntity(
    id = this.id,
    customerId = this.customerId,
    returnedDate = this.returnedDate,
    userId = this.userId,
    latitude = this.latitude,
    longitude = this.longitude,
    isSynced = isSynced,
    invoiceNum = invoiceNum,
    updatedAt = updateAt
)
fun InboundDetailesEntity.toDto(): InboundDetailsDto{
    return InboundDetailsDto(
        id=this.id,
        inbound_id = this.InboundId,
        item_id = this.ItemId,
        amount = this.amount,
        updatedAt
    )
}

// تحويل من Domain (Customer) إلى Dto (السيرفر)

// تحويل من Dto (السيرفر) إلى Entity (قاعدة البيانات المحلية)
fun CustomerDto.toEntity(): Customer{
    return Customer(
        id = id,
        customerName = customer_name,

        updatedAt = updated_at,
        userId = user_id,
        customerNum =customer_num,
        customerDebt = customer_debt,
        firstCustomerDebt = firstCustomerDebt,
        isSync = is_sync,
    )
}

// تحويل من Entity (Room) إلى Domain (للتعامل معه في التطبيق)

// تحويل من Entity (Room) إلى DTO (Supabase)
fun ReturnedEntity.toDto() = ReturnedDto(
    customer_id = customerId,
    returned_date = returnedDate,
    latitude = latitude,
    longitude = longitude,
    user_id = this.userId,
    id = id,
    updated_at = updatedAt,
    invoiceNum = invoiceNum
)

fun ReturnedDetailsEntity.toDto() = ReturnedDetailsDto(
    returned_id = returnedId,
    item_id = itemId,
    amount = amount,
    price = price,
    id = id,
    updated_at =updatedAt
)

fun ReturnedDetailsDto.toEntity() = ReturnedDetailsEntity(
    returnedId = returned_id,
    itemId = item_id,
    amount = amount,
    price = price,
updatedAt = updated_at,
    id = id

)
fun InboundEntity.toDto(): InboundDto{
    return InboundDto(
        user_id = this.userId,
        invose_id = this.invorseNum,
        image_url = this.image,
        inbound_date = this.inboundDate.toString().formatToEnglish(),
        latitude = this.latitude,
        longitude = this.longitude,
        is_synced = this.isSynced,
        id = this.id,
        fromSupplied_id = this.fromSppliedId,

    )
}

// تحويل من Model (Domain) إلى Entity (Database)
fun ReturnedDetailsModel.toEntity(returnedId: String): ReturnedDetailsEntity {
    return ReturnedDetailsEntity(
        id = this.id, // لو 0 الـ Room هيعمل Auto-generate
        returnedId = returnedId,
        itemId = this.itemId,
        amount = this.amount,
        price = this.price,
        updatedAt = System.currentTimeMillis()
    )
}

// تحويل من Entity (Database) إلى Model (Domain) - لو هتحتاجه في العرض
fun ReturnedDetailsEntity.toDomain(itemName: String = ""): ReturnedDetailsModel {
    return ReturnedDetailsModel(
        id = this.id,
        returnedId = this.returnedId,
        itemId = this.itemId,
        amount = this.amount,
        price = this.price,
        itemName = itemName // بنجيب الاسم عادة من Join في الـ DAO
    )
}
// --- تحويل Stock ---
fun StockEntity.toDomain(): Stock {
    return Stock(
        id = this.id,
        ItemId = this.ItemId,
        userId = this.userId,
        InitAmount = this.InitAmount,
        CurrentAmount = this.CurrentAmount,
        fristDate = this.fristDate.toString().formatToEnglish(),
        isSynced = this.isSynced
    )
}


// تحويل كائن الـ Query (الذي يحتوي على الاسم) إلى كائن الـ Domain
fun StockWithItemName.toDomain(): Stock {
    return Stock(
        id = this.id,
        ItemId = this.ItemId,
        itemName = this.itemName, // هنا الاسم الذي جلبناه من جدول الأصناف
        CurrentAmount = this.CurrentAmount,
        fristDate = this.fristDate.toString().formatToEnglish(),
        userId = "",
        isSynced = false,
        InitAmount=this.InitAmount

    )
}
fun InboundDetailsDto.toEntity(): InboundDetailesEntity {
    return InboundDetailesEntity(
        id = this.id ,
        ItemId = this.item_id,
        amount = this.amount,
        InboundId = this.inbound_id,
        isSynced = true,
    )
}
// تحويل من Entity (قاعدة البيانات) إلى Domain (المنطق والواجهات)
fun Supplied.toDomain(): SuppliedModel {
    return SuppliedModel(
        id = this.id,
        suppliedName = this.suppliedName,
        num = this.num,
    )
}
fun Transfer.toEntity(): TransferEntity {
    return TransferEntity(
        id = this.id,
        transferNum = this.transferNum,
        fromStoreId = this.fromStoreId,
        toStoreId = this.toStoreId,
        date = this.date,
        userId = this.userId,
        isSynced = this.isSynced
    )
}
fun TransferEntity.toDto() = TransferDto(
    fromStoreId = fromStoreId,
    toStoreId = toStoreId,
    transferNum = transferNum,
    date = date,
    userId = userId,
    id = id,
)

fun TransferDto.toEntity() = TransferEntity(
    id=id,
    fromStoreId = fromStoreId,
    toStoreId = toStoreId,
    transferNum = transferNum,
    date = date,
    userId = userId
)

fun TransferDetailsEntity.toDto() = TransferDetailDto(
    itemId = itemId,
    amount = amount,
    transferId = transferId,
    id = id,
    updated_at =updatedAt,
)
// تحويل TransferDetails (Domain) إلى TransferDetailsEntity (Database)
fun TransferDetails.toEntity(): TransferDetailsEntity {
    return TransferDetailsEntity(
        id = this.id,
        transferId = this.transferId,
        itemId = this.itemId,
        amount = this.amount,

    )
}
fun InboundDto.toEntity(): InboundEntity {
    return InboundEntity(
        id = this.id , // إذا كان الـ id نل من السيرفر، نعطيه 0 ليقوم Room بتوليده
        userId = this.user_id,
        isSynced = true,
        invorseNum = this.invose_id,
        fromSppliedId = this.fromSupplied_id,
        image = this.image_url,
        inboundDate = this.inbound_date,
        latitude = this.latitude,
        longitude = this.longitude // بما أنها قادمة من السيرفر فهي مزامنة بالفعل
    )
}
fun ItemsEntity.toDomain(): Items {
    return Items(
        id = this.id,
        itemName = this.itemName,
        itemNum = this.itemNum
    )
}

// تحويل من Domain Model إلى Entity (لحفظ صنف جديد مثلاً)
fun Items.toEntity(): ItemsEntity {
    return ItemsEntity(
        id = this.id,
        itemName = this.itemName,
        itemNum = this.itemNum
    )
}

fun Stock.toEntity(): StockEntity {
    return StockEntity(
        id = this.id,
        ItemId = this.ItemId,
        userId = this.userId,
        InitAmount = this.InitAmount,
        CurrentAmount = this.CurrentAmount,
        fristDate = this.fristDate.toString().formatToEnglish(),
        isSynced = this.isSynced
    )
}