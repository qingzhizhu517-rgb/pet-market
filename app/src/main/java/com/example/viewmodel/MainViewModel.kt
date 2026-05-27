package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PetTab {
    MARKET,
    WIKI,
    AI_CHAT,
    FAVORITES
}

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val petDao = db.petDao()

    // --- Tab Management ---
    private val _currentTab = MutableStateFlow(PetTab.MARKET)
    val currentTab: StateFlow<PetTab> = _currentTab.asStateFlow()

    fun selectTab(tab: PetTab) {
        _currentTab.value = tab
    }

    // --- Market Tab Filters & State ---
    private val _marketCategoryFilter = MutableStateFlow("All") // "All", "Dog", "Cat"
    val marketCategoryFilter: StateFlow<String> = _marketCategoryFilter.asStateFlow()

    private val _marketSearchQuery = MutableStateFlow("")
    val marketSearchQuery: StateFlow<String> = _marketSearchQuery.asStateFlow()

    fun setMarketCategoryFilter(category: String) {
        _marketCategoryFilter.value = category
    }

    fun setMarketSearchQuery(query: String) {
        _marketSearchQuery.value = query
    }

    // Combine all listing flows with filter + query!
    val filteredListings: StateFlow<List<PetListing>> = combine(
        petDao.getAllListings(),
        _marketCategoryFilter,
        _marketSearchQuery
    ) { listings, category, query ->
        var list = listings
        if (category != "All") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            list = list.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.breedName.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Breed Wiki Filters & State ---
    private val _wikiCategoryFilter = MutableStateFlow("All") // "All", "Dog", "Cat"
    val wikiCategoryFilter: StateFlow<String> = _wikiCategoryFilter.asStateFlow()

    private val _wikiSearchQuery = MutableStateFlow("")
    val wikiSearchQuery: StateFlow<String> = _wikiSearchQuery.asStateFlow()

    fun setWikiCategoryFilter(category: String) {
        _wikiCategoryFilter.value = category
    }

    fun setWikiSearchQuery(query: String) {
        _wikiSearchQuery.value = query
    }

    val filteredBreeds: StateFlow<List<BreedInfo>> = combine(
        flowOf(BreedData.breeds),
        _wikiCategoryFilter,
        _wikiSearchQuery
    ) { breeds, category, query ->
        var list = breeds
        if (category != "All") {
            list = list.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            list = list.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.englishName.contains(query, ignoreCase = true) ||
                it.temperamentTags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BreedData.breeds)

    // --- Favorites State ---
    val favoritesList: StateFlow<List<FavoriteItem>> = petDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Selected details modal states ---
    private val _selectedListing = MutableStateFlow<PetListing?>(null)
    val selectedListing: StateFlow<PetListing?> = _selectedListing.asStateFlow()

    private val _selectedBreed = MutableStateFlow<BreedInfo?>(null)
    val selectedBreed: StateFlow<BreedInfo?> = _selectedBreed.asStateFlow()

    fun selectListing(listing: PetListing?) {
        _selectedListing.value = listing
    }

    fun selectBreed(breed: BreedInfo?) {
        _selectedBreed.value = breed
    }

    // Check live if a breed/listing id is in favorites
    fun isFavoriteFlow(id: String): Flow<Boolean> {
        return petDao.isFavorite(id).map { count -> count > 0 }
    }

    fun toggleFavorite(id: String, resourceType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = petDao.isFavorite(id).first()
            if (count > 0) {
                petDao.deleteFavorite(id)
            } else {
                petDao.insertFavorite(FavoriteItem(id = id, resourceType = resourceType))
            }
        }
    }

    // --- AI Chat Tab State ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "您好！我是您的宠物集市智能助手 🐾。关于猫狗品种、科学喂养、犬猫挑选或日常护理的任何疑问，都可以问我哦！\n\n试试下方推荐的主题快速开聊吧！"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        
        val newMessages = _chatMessages.value.toMutableList()
        newMessages.add(ChatMessage(sender = "user", text = userText))
        _chatMessages.value = newMessages

        _isAiGenerating.value = true

        viewModelScope.launch {
            val systemInstructions = """
                您是专业的“宠物集市”AI科学喂养与品种百科顾问。
                请用温暖友好、富有耐心、科学严谨、易于理解的中文语气回答问题。
                主要回答关于猫咪、狗狗的品种标准、习惯品性、日常生活照料、科学膳食（如各阶段喂食分量、禁忌食物）、新手挑选宠物建议等。
                如果问题与猫狗或宠物完全无关，请在礼貌回答后，引导用户回到猫狗繁育、品种、喂养或如何挑选健康宠物的话题上来。
            """.trimIndent()

            val contents = newMessages.map { msg ->
                Content(parts = listOf(Part(text = msg.text)))
            }

            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    // Friendly offline simulated answer fallback if API Key is not set yet
                    val petResponse = withContext(Dispatchers.IO) {
                        simulateFallbackResponse(userText)
                    }
                    _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = petResponse)
                } else {
                    val request = GeminiRequest(
                        contents = contents,
                        generationConfig = GenerationConfig(temperature = 0.7f, maxOutputTokens = 1200),
                        systemInstruction = Content(parts = listOf(Part(text = systemInstructions)))
                    )
                    val response = withContext(Dispatchers.IO) {
                        GeminiClient.service.generateContent(apiKey, request)
                    }
                    val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "抱歉，没能理解您的意思，请试着换种描述问我关于宠物的知识吧。"
                    _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = aiText)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "AI Error", e)
                val isNetworkError = e.message?.contains("Unable to resolve host") == true
                val friendlyError = if (isNetworkError) {
                    "提示：网络连接似乎有些波动。为您启用本地离线库回答：\n\n" + simulateFallbackResponse(userText)
                } else {
                    "加载AI问答时遇到了错误: ${e.localizedMessage ?: "连接失败"}。已自动启用本地百科答案：\n\n" + simulateFallbackResponse(userText)
                }
                _chatMessages.value = _chatMessages.value + ChatMessage(sender = "ai", text = friendlyError, isError = true)
            } finally {
                _isAiGenerating.value = false
            }
        }
    }

    private fun simulateFallbackResponse(query: String): String {
        return when {
            query.contains("金毛") || query.contains("狗") && query.contains("钙") -> {
                "【金毛犬补钙科学建议】\n1. **成长期补钙**：金毛属于大型犬，幼犬在3-10个月骨骼发育最快，每日建议搭配富含钙和维生素D3的专用宠钙粉；\n2. **食补配方**：可喂食适量无蔗糖酸奶、水煮蛋黄、三文鱼皮、虾皮粉补钙；\n3. **阳光与运动**：补钙必须有适量阳光照射产生内源性维D3！建议每天散步30-60分钟；\n4. **切忌过量**：过量补钙会导致幼犬软骨过早钙化和髋骨畸形。请遵照说明书适度补给。"
            }
            query.contains("上班族") || query.contains("忙") || query.contains("猫") && query.contains("推荐") -> {
                "【适合朝九晚五打工人的猫咪推荐】\n1. **英国短毛猫 (英短)**：首选！性格超级沉稳、独立，非常耐得住寂寞，不会因为家里没人就沮丧或乱叫，能自怡自得玩耍或睡觉。\n2. **美国短毛猫 (美短)**：抵抗力极佳，不娇气，精力适度，不容易焦虑，对环境适应极快。\n3. **中华狸花猫**：生命力极其顽强，不挑食、很少生病，是极好打理的暖心伴侣。\n*提示：打工人养猫请每天下班后抽空10分钟用逗猫棒进行亲密互动，并保证家里猫砂盆时刻保持清洁。*"
            }
            query.contains("新手") || query.contains("准备") || query.contains("第一次") -> {
                "【新手养猫狗全套入坑清单】\n1. **饮食餐具**：双色防滑大猫狗碗（不锈钢或陶瓷防黑下巴）、正规合格主粮、充足的纯净软化水；\n2. **生活用具**：宠物指甲剪、不伤皮肤的高密按摩钢梳、宠物洗耳液、免洗泡沫；\n3. **环境卫生**：猫咪必备封闭式猫砂盆+无粉尘猫砂；狗狗则是舒适狗窝+固定大小便诱导餐垫；\n4. **安全保护**：猫咪必须装好防坠纱窗！狗狗出门务必搭配防勒颈牵引防爆冲胸背带；\n5. **医疗防护**：记录下周围24小时宠物医院，领养前做个全面的寄生虫驱杀与核心多联病疫苗抗体筛查！"
            }
            query.contains("布偶") -> {
                "【布偶猫（Ragdoll）科学呵护指南】\n1. **玻璃胃呵护**：布偶猫因基因问题肠胃较弱易拉稀。幼龄期建议长期备有益生菌，换粮务必遵循10天七成渐进换粮法。\n2. **毛发梳理**：布偶拥有浓密的长毛，腋下、大腿内侧容易打结，务必每天早晚各用排梳梳理一次，帮助排出舔进胃里的毛发。\n3. **温度调适**：布偶适应温和室内，夏季超过28℃度易应激，建议开启冷风空调，保持室内通风舒适。"
            }
            else -> {
                "“喵~汪~” 智能百科里有丰富的知识储备！您可以针对以下话题继续提问：\n1. **「品种特征」**：例如“我想了解柯基的性格特点”\n2. **「健康喂食」**：例如“猫咪可以吃巧克力吗？”或“金毛每天应该喂多少狗粮？”\n3. **「挑选建议」**：例如“怎样在宠物店挑一只健康的幼犬？”\n4. **「不良坏习惯纠正」**：例如“狗狗喜欢拆家咬沙发怎么办？”"
            }
        }
    }

    // --- Create Custom Listing Form Submit ---
    fun addNewListing(
        name: String,
        breedName: String,
        category: String,
        age: String,
        gender: String,
        priceText: String,
        city: String,
        description: String,
        phone: String,
        weChat: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val placeholderUrls = if (category.equals("Cat", ignoreCase = true)) {
                listOf(
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?q=80&w=500&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?q=80&w=500&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1574158622643-69d34d72650a?q=80&w=500&auto=format&fit=crop"
                )
            } else if (category.equals("Small", ignoreCase = true)) {
                listOf(
                    "https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?q=80&w=500&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1425082661705-1834bfd09dca?q=80&w=500&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1534361960057-19889db9621e?q=80&w=500&auto=format&fit=crop"
                )
            } else {
                listOf(
                    "https://images.unsplash.com/photo-1552053831-71594a27632d?q=80&w=500&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1612536057832-2ff7eed58194?q=80&w=500&auto=format&fit=crop",
                    "https://images.unsplash.com/photo-1583511655826-05700d52f4d9?q=80&w=500&auto=format&fit=crop"
                )
            }
            val randomImage = placeholderUrls.random()

            val newListing = PetListing(
                name = name,
                breedName = breedName,
                category = category,
                age = age,
                gender = gender,
                priceText = priceText,
                city = city,
                description = description,
                imageUrl = randomImage,
                contactPhone = phone,
                contactWeChat = weChat,
                isUserPosted = true
            )
            petDao.insertListing(newListing)
        }
    }

    // --- Init / Seeding Process ---
    init {
        viewModelScope.launch {
            // Check if there are existing listings. If empty, seed standard ones!
            petDao.getAllListings().first().let { currentList ->
                if (currentList.isEmpty()) {
                    seedInitialData()
                }
            }
        }
    }

    private suspend fun seedInitialData() {
        val seeds = listOf(
            PetListing(
                name = "可乐 (萌宠金毛幼犬)",
                breedName = "金毛寻回犬",
                category = "Dog",
                age = "3个月",
                gender = "公",
                priceText = "免费领养",
                city = "上海市",
                description = "自家金毛大狗生的一窝小奶狗，性格活泼可爱。超级粘人听话，已经接种了第一针疫苗。因为精力有限养不下这么多只，寻找懂狗、有爱心、能陪伴它一生的主人！要求不抛弃、能科学喂养，有稳定的住宅和稳定的经济来源。",
                imageUrl = "https://images.unsplash.com/photo-1552053831-71594a27632d?q=80&w=500&auto=format&fit=crop",
                contactPhone = "13588889999",
                contactWeChat = "golden_cola_love"
            ),
            PetListing(
                name = "年糕 (无敌圆脸包子英短)",
                breedName = "英国短毛猫",
                category = "Cat",
                age = "8个月",
                gender = "公",
                priceText = "¥1,500",
                city = "北京市",
                description = "超级罕见的极品小胖子。圆鼓鼓的腮帮子像个松饼。性格极度慵懒温顺，随便你怎么捏都很配合。性格非常独立，白天大人去上班它就乖乖睡觉，绝对不发出任何噪音。驱虫及三针疫苗已全部做完，带绿本。",
                imageUrl = "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?q=80&w=500&auto=format&fit=crop",
                contactPhone = "18633334444",
                contactWeChat = "british_nyangao"
            ),
            PetListing(
                name = "奶糖 (荷兰侏儒掌心小白兔)",
                breedName = "荷兰侏儒兔",
                category = "Small",
                age = "2个月",
                gender = "母",
                priceText = "¥260",
                city = "广州市",
                description = "纯种双色熊猫侏儒兔。体型娇小，成年后最多一公斤出头，耳朵超短立着，大眼睛圆溜溜极其软萌。平时非常爱干净，会上厕所、喜欢吃提摩西牧草和兔粮。极其安静乖巧，是极佳的桌面治愈伴侣。赠送定制双层兔笼、原装磨牙石和一千克干草包。",
                imageUrl = "https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?q=80&w=500&auto=format&fit=crop",
                contactPhone = "13422223333",
                contactWeChat = "rabbit_sugar_babe"
            ),
            PetListing(
                name = "雪球 (开脸对称大双色布偶)",
                breedName = "布偶猫",
                category = "Cat",
                age = "4个月",
                gender = "母",
                priceText = "¥2,800",
                city = "杭州市",
                description = "高品质仙女级海双色布偶猫，两只深邃的大蓝眼睛像极了星辰大海。性格比水还要温柔，一到怀里就会发出呼噜呼噜的和善回响。非常干净，会熟练操作各种猫砂。带有纯种五代血统证书，希望为尊贵的小公主寻觅靠谱新居。",
                imageUrl = "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?q=80&w=500&auto=format&fit=crop",
                contactPhone = "15966667777",
                contactWeChat = "doll_snowball_queen"
            ),
            PetListing(
                name = "汤圆 (极品三线胖仓鼠)",
                breedName = "加卡利亚仓鼠 (三线/银狐)",
                category = "Small",
                age = "1.5个月",
                gender = "公",
                priceText = "¥20",
                city = "南京市",
                description = "超级呆萌搞笑的三线鼠宝宝，平时最爱捧着葵花子和松子往自家的颊囊里拼命塞得鼓鼓的。性格温驯亲人，可以在主人掌心里四脚朝天睡大觉，互动脱敏已全部做好，绝对不咬人。超低门槛的租房桌边守护神！",
                imageUrl = "https://images.unsplash.com/photo-1425082661705-1834bfd09dca?q=80&w=500&auto=format&fit=crop",
                contactPhone = "18055551212",
                contactWeChat = "hamster_sweet_ball"
            ),
            PetListing(
                name = "福仔 (表情包治愈系柴犬)",
                breedName = "柴犬",
                category = "Dog",
                age = "5个月",
                gender = "公",
                priceText = "¥1,200",
                city = "深圳市",
                description = "极度开朗可爱的赤柴宝宝，网红微笑脸随时挂着，简直是解压神器。平时非常爱干净，像猫一样懂得整理自洁。不会无缘无故吠叫。因本人近期工作被外派到出国，不得已忍痛割爱。附赠全新大型豪华航空箱、精美胸背带和3包优质狗粮。",
                imageUrl = "https://images.unsplash.com/photo-1583511655826-05700d52f4d9?q=80&w=500&auto=format&fit=crop",
                contactPhone = "13011112222",
                contactWeChat = "shiba_fuzai_happy"
            )
        )
        for (seed in seeds) {
            petDao.insertListing(seed)
        }
    }
}
