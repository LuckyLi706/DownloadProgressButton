package com.lucky.downloadprogressbutton

import android.animation.Animator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView

/**
 * 下载按钮
 */
class DownloadProgressButton constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {
    interface OnDownLoadClickListener {
        fun clickDownload()
        fun clickPause()
        fun clickResume()
        fun clickFinish()
    }

    //背景画笔
    private var mBackgroundPaint: Paint? = null

    //背景边框画笔
    private var mBackgroundBorderPaint: Paint? = null

    //按钮文字画笔
    @Volatile
    private var mTextPaint: Paint? = null

    //背景颜色
    private var mBackgroundColor = 0

    //下载中后半部分后面背景颜色
    private var mBackgroundSecondColor = 0

    //文字颜色
    private var mTextColor = 0

    //覆盖后颜色
    var textCoverColor = 0
    private var mProgress = -1f
    private var mToProgress = 0f
    var maxProgress = 0
    var minProgress = 0
    private var mProgressPercent = 0f
    var buttonRadius = 0f
    private var mBackgroundBounds: RectF? = null
    private var mProgressBgGradient: LinearGradient? = null
    private var mProgressTextGradient: LinearGradient? = null
    private var mProgressAnimation: ValueAnimator? = null
    private var mCurrentText: CharSequence? = null
    private var mState = -1
    private var backgroud_strokeWidth //边框宽度
            = 0f
    private var mNormalText: String? = null
    private var mDowningText: String? = null
    private var mFinishText: String? = null
    private var mPauseText: String? = null
    private var mAnimationDuration: Long = 0
    var onDownLoadClickListener: OnDownLoadClickListener? = null
    var isEnablePause = false
    private var mEnableDownload = false
    private fun initAttrs(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DownloadProgressButton)
        mBackgroundColor = a.getColor(
            R.styleable.DownloadProgressButton_background_color,
            Color.parseColor("#6699ff")
        )
        mBackgroundSecondColor =
            a.getColor(R.styleable.DownloadProgressButton_background_second_color, Color.LTGRAY)
        buttonRadius =
            a.getFloat(R.styleable.DownloadProgressButton_radius, (measuredHeight / 2).toFloat())
        mTextColor = a.getColor(R.styleable.DownloadProgressButton_text_color, mBackgroundColor)
        textCoverColor =
            a.getColor(R.styleable.DownloadProgressButton_text_cover_color, Color.WHITE)
        backgroud_strokeWidth =
            a.getDimension(R.styleable.DownloadProgressButton_background_strokeWidth, 0f)
        mNormalText = a.getString(R.styleable.DownloadProgressButton_text_normal)
        mDowningText = a.getString(R.styleable.DownloadProgressButton_text_downing)
        mFinishText = a.getString(R.styleable.DownloadProgressButton_text_finish)
        mPauseText = a.getString(R.styleable.DownloadProgressButton_text_pause)
        mAnimationDuration =
            a.getInt(R.styleable.DownloadProgressButton_animation_duration, 500).toLong()
        a.recycle()
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        mTextPaint!!.textSize = textSize
        invalidate()
    }

    private fun init() {
        maxProgress = 100
        minProgress = 0
        mProgress = 0f
        if (mNormalText == null) {
            mNormalText = "下载"
        }
        if (mDowningText == null) {
            mDowningText = "进度"
        }
        if (mFinishText == null) {
            mFinishText = "安装"
        }
        if (mPauseText == null) {
            mPauseText = "继续"
        }
        //设置背景画笔
        mBackgroundPaint = Paint()
        mBackgroundPaint!!.isAntiAlias = true
        mBackgroundPaint!!.style = Paint.Style.FILL
        mBackgroundBorderPaint = Paint()
        mBackgroundBorderPaint!!.isAntiAlias = true
        mBackgroundBorderPaint!!.style = Paint.Style.STROKE
        mBackgroundBorderPaint!!.strokeWidth = backgroud_strokeWidth
        mBackgroundBorderPaint!!.color = mBackgroundColor
        //设置文字画笔
        mTextPaint = Paint()
        mTextPaint!!.isAntiAlias = true
        //解决文字有时候画不出问题
        setLayerType(LAYER_TYPE_SOFTWARE, mTextPaint)

        //初始化状态设为NORMAL
        state = NORMAL
        setOnClickListener(object : OnClickListener {
            override fun onClick(v: View) {
                if (onDownLoadClickListener == null) {
                    return
                }
                if (state == NORMAL) {
                    onDownLoadClickListener!!.clickDownload()
                    if (mEnableDownload) {
                        state = DOWNLOADING
                        setProgressText(0)
                    }
                } else if (state == DOWNLOADING) {
                    if (isEnablePause) {
                        onDownLoadClickListener!!.clickPause()
                        state = PAUSE
                    }
                } else if (state == PAUSE) {
                    onDownLoadClickListener!!.clickResume()
                    state = DOWNLOADING
                    setProgressText(mProgress.toInt())
                } else if (state == FINISH) {
                    onDownLoadClickListener!!.clickFinish()
                }
            }
        })
    }

    private fun setupAnimations() {
        mProgressAnimation = ValueAnimator.ofFloat(0f, 1f).setDuration(mAnimationDuration)
        mProgressAnimation?.addUpdateListener(AnimatorUpdateListener { animation ->
            val timePercent = animation.animatedValue as Float
            mProgress += (mToProgress - mProgress) * timePercent
            setProgressText(mProgress.toInt())
        })
        mProgressAnimation?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (mToProgress < mProgress) {
                    mProgress = mToProgress
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                if (mProgress == maxProgress.toFloat()) {
                    state = FINISH
                }
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isInEditMode) {
            drawing(canvas)
        }
    }

    private fun drawing(canvas: Canvas) {
        drawBackground(canvas)
        drawTextAbove(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        if (mBackgroundBounds == null) {
            mBackgroundBounds = RectF()
            if (buttonRadius == 0f) {
                buttonRadius = (measuredHeight / 2).toFloat()
            }
            mBackgroundBounds!!.left = backgroud_strokeWidth
            mBackgroundBounds!!.top = backgroud_strokeWidth
            mBackgroundBounds!!.right = measuredWidth - backgroud_strokeWidth
            mBackgroundBounds!!.bottom = measuredHeight - backgroud_strokeWidth
        }
        when (mState) {
            NORMAL -> {
                if (mBackgroundPaint!!.shader != null) {
                    mBackgroundPaint!!.shader = null
                }
                mBackgroundPaint!!.color = mBackgroundColor
                canvas.drawRoundRect(
                    mBackgroundBounds!!,
                    buttonRadius,
                    buttonRadius,
                    mBackgroundPaint!!
                )
            }
            DOWNLOADING, PAUSE -> {
                mProgressPercent = mProgress / (maxProgress + 0f)
                mProgressBgGradient = LinearGradient(
                    backgroud_strokeWidth,
                    0f,
                    measuredWidth - backgroud_strokeWidth,
                    0f,
                    intArrayOf(mBackgroundSecondColor, mBackgroundColor),
                    floatArrayOf(mProgressPercent, mProgressPercent + 0.001f),
                    Shader.TileMode.CLAMP
                )
                mBackgroundPaint!!.color = mBackgroundColor
                mBackgroundPaint!!.shader = mProgressBgGradient
                canvas.drawRoundRect(
                    mBackgroundBounds!!,
                    buttonRadius,
                    buttonRadius,
                    mBackgroundPaint!!
                )
            }
            FINISH -> {
                mBackgroundPaint!!.shader = null
                mBackgroundPaint!!.color = mBackgroundColor
                canvas.drawRoundRect(
                    mBackgroundBounds!!,
                    buttonRadius,
                    buttonRadius,
                    mBackgroundPaint!!
                )
            }
        }
        canvas.drawRoundRect(
            mBackgroundBounds!!,
            buttonRadius,
            buttonRadius,
            mBackgroundBorderPaint!!
        ) //绘制边框
    }

    private fun drawTextAbove(canvas: Canvas) {
        mTextPaint!!.textSize = textSize
        val y = canvas.height / 2 - (mTextPaint!!.descent() / 2 + mTextPaint!!.ascent() / 2)
        if (mCurrentText == null) {
            mCurrentText = ""
        }
        val textWidth = mTextPaint!!.measureText(mCurrentText.toString())
        mTextPaint!!.style = Paint.Style.FILL_AND_STROKE
        mTextPaint!!.strokeWidth = 0.9f
        mTextPaint!!.isAntiAlias = true
        when (mState) {
            NORMAL -> {
                mTextPaint!!.shader = null
                mTextPaint!!.color = mTextColor
                canvas.drawText(
                    mCurrentText.toString(),
                    (measuredWidth - textWidth) / 2,
                    y,
                    mTextPaint!!
                )
            }
            DOWNLOADING, PAUSE -> {
                val w = measuredWidth - 2 * backgroud_strokeWidth
                //进度条压过距离
                val coverlength = w * mProgressPercent
                //开始渐变指示器
                val indicator1 = w / 2 - textWidth / 2
                //结束渐变指示器
                val indicator2 = w / 2 + textWidth / 2
                //文字变色部分的距离
                val coverTextLength = textWidth / 2 - w / 2 + coverlength
                val textProgress = coverTextLength / textWidth
                if (coverlength <= indicator1) {
                    mTextPaint!!.shader = null
                    mTextPaint!!.color = mTextColor
                } else if (indicator1 < coverlength && coverlength <= indicator2) {
                    mProgressTextGradient = LinearGradient(
                        (w - textWidth) / 2 + backgroud_strokeWidth,
                        0f,
                        (w + textWidth) / 2 + backgroud_strokeWidth,
                        0f,
                        intArrayOf(textCoverColor, mTextColor),
                        floatArrayOf(textProgress, textProgress + 0.001f),
                        Shader.TileMode.CLAMP
                    )
                    mTextPaint!!.color = mTextColor
                    mTextPaint!!.shader = mProgressTextGradient
                } else {
                    mTextPaint!!.shader = null
                    mTextPaint!!.color = textCoverColor
                }
                canvas.drawText(
                    mCurrentText.toString(),
                    (w - textWidth) / 2 + backgroud_strokeWidth,
                    y,
                    mTextPaint!!
                )
            }
            FINISH -> {
                mTextPaint!!.color = mTextColor
                canvas.drawText(
                    mCurrentText.toString(),
                    (measuredWidth - textWidth) / 2,
                    y,
                    mTextPaint!!
                )
            }
        }
    }

    //状态确实有改变
    var state: Int
        get() = mState
        set(state) {
            if (mState != state) { //状态确实有改变
                mState = state
                when (state) {
                    FINISH -> {
                        currentText = mFinishText
                        mProgress = maxProgress.toFloat()
                    }
                    NORMAL -> {
                        mToProgress = minProgress.toFloat()
                        mProgress = mToProgress
                        currentText = mNormalText
                    }
                    PAUSE -> {
                        currentText = mPauseText
                    }
                }
                invalidate()
            }
        }

    fun reset() {
        state = NORMAL
    }

    fun finish() {
        state = FINISH
    }

    var currentText: CharSequence?
        get() = mCurrentText
        set(charSequence) {
            mCurrentText = charSequence
            invalidate()
        }
    var progress: Float
        get() = mProgress
        set(progress) {
            if (progress <= minProgress || progress <= mToProgress || state == FINISH) {
                return
            }
            mToProgress = Math.min(progress, maxProgress.toFloat())
            state = DOWNLOADING
            if (mProgressAnimation!!.isRunning) {
                mProgressAnimation!!.end()
                mProgressAnimation!!.start()
            } else {
                mProgressAnimation!!.start()
            }
        }

    private fun setProgressText(progress: Int) {
        if (state == DOWNLOADING) {
            currentText = "$mDowningText$progress%"
        }
    }

    fun pause() {
        state = PAUSE
    }

    fun getTextColor(): Int {
        return mTextColor
    }

    override fun setTextColor(textColor: Int) {
        mTextColor = textColor
    }

    var animationDuration: Long
        get() = mAnimationDuration
        set(animationDuration) {
            mAnimationDuration = animationDuration
            mProgressAnimation!!.duration = animationDuration
        }

    fun setEnableDownload(enableDownload: Boolean) {
        mEnableDownload = enableDownload
    }

    companion object {
        const val NORMAL = 1
        const val DOWNLOADING = 2
        const val PAUSE = 3
        const val FINISH = 4
    }

    init {
        if (!isInEditMode) {
            initAttrs(context, attrs)
            init()
            setupAnimations()
        }
    }
}