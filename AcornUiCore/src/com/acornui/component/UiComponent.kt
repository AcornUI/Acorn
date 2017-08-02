/*
 * Copyright 2015 Nicholas Bilyk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.acornui.component

import com.acornui.assertionsEnabled
import com.acornui.collection.*
import com.acornui.component.layout.LayoutData
import com.acornui.component.layout.SizeConstraints
import com.acornui.component.layout.SizeConstraintsRo
import com.acornui.component.style.*
import com.acornui.core.*
import com.acornui.core.assets.AssetManager
import com.acornui.core.di.*
import com.acornui.core.graphics.Camera
import com.acornui.core.graphics.Window
import com.acornui.core.input.InteractionEvent
import com.acornui.core.input.InteractionType
import com.acornui.core.input.InteractivityManager
import com.acornui.core.input.MouseState
import com.acornui.core.time.TimeDriver
import com.acornui.graphics.Color
import com.acornui.graphics.ColorRo
import com.acornui.math.*
import com.acornui.signal.Signal
import com.acornui.signal.Signal2
import com.acornui.signal.StoppableSignal
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.firstOrNull
import kotlin.collections.isNotEmpty
import kotlin.collections.set

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ComponentDslMarker

typealias ComponentInit<T> = (@ComponentDslMarker T).() -> Unit

interface UiComponent : Lifecycle, ColorTransformable, InteractiveElement, Validatable, Styleable {

	override val owner: Owned

	/**
	 * The parent on the display graph.
	 */
	override var parent: Container?

	override val invalidated: Signal<(UiComponent, Int) -> Unit>

	/**
	 * Returns true if this component will be rendered. This will be true under the following conditions:
	 * This component is on the stage.
	 * This component and all of its ancestors are visible.
	 * This component does not have an alpha of 0f.
	 */
	fun isRendered(): Boolean {
		if (!isActive) return false
		if (concatenatedColorTint.a <= 0f) return false
		var p: UiComponent? = this
		while (p != null) {
			if (!p.visible) return false
			p = p.parent
		}
		return true
	}

	/**
	 * If false, this component will not be rendered, interact with user input, included in layouts, or included in
	 * focus order.
	 */
	var visible: Boolean

	/**
	 * Set this to false to make this layout element not included in layout algorithms.
	 */
	var includeInLayout: Boolean

	fun onAncestorVisibleChanged(uiComponent: UiComponent, value: Boolean)

	/**
	 * Given a global position, casts a ray in the direction of the camera, returning the deepest enabled interactive
	 * element at that position.
	 * If there are multiple objects at this position, only the top-most object is returned. (by child index, not z
	 * value)
	 */
	fun getChildUnderPoint(canvasX: Float, canvasY: Float, onlyInteractive: Boolean): UiComponent? {
		val ray = Ray.obtain()
		camera.getPickRay(canvasX, canvasY, ray)
		val element = getChildUnderRay(ray, onlyInteractive)
		ray.free()
		return element
	}

	@Suppress("UNCHECKED_CAST") fun getChildUnderRay(globalRay: Ray, onlyInteractive: Boolean): UiComponent? {
		val tmpList = arrayListPool.obtain() as ArrayList<UiComponent>
		getChildrenUnderRay(globalRay, tmpList, onlyInteractive = onlyInteractive, returnAll = false)
		val first = tmpList.firstOrNull()
		arrayListPool.free(tmpList)
		return first
	}

	/**
	 * Given a screen position, casts a ray in the direction of the camera, populating the [out] list with the
	 * components
	 *
	 * @param canvasX The x coordinate relative to the canvas.
	 * @param canvasY The y coordinate relative to the canvas.
	 * @param out The array list to populate with elements.
	 */
	fun getChildrenUnderPoint(canvasX: Float, canvasY: Float, out: ArrayList<UiComponent>, onlyInteractive: Boolean): ArrayList<UiComponent> {
		val ray = Ray.obtain()
		camera.getPickRay(canvasX, canvasY, ray)
		getChildrenUnderRay(ray, out, onlyInteractive)
		ray.free()
		return out
	}

	/**
	 * @param globalRay The ray in global coordinate space to check for intersections.
	 * @param out The array list to populate with the interactive elements that match the criteria.
	 * @param returnAll If true, all intersecting elements will be added to [out], if false, only the top-most element.
	 * @param onlyInteractive If true, only elements where [InteractiveElement.interactivityEnabled] is true.
	 *
	 * This method will not return elements where [UiComponent.visible] is false.
	 */
	fun getChildrenUnderRay(globalRay: Ray, out: ArrayList<UiComponent>, onlyInteractive: Boolean, returnAll: Boolean = true)

	/**
	 * Updates this component, validating it and its children.
	 */
	fun update()

	/**
	 * Render any graphics.
	 */
	fun render()

}

/**
 * The base for every AcornUi component.
 * UiComponent provides lifecycle, validation, interactivity, transformation, and layout.
 *
 * @author nbilyk
 */
open class UiComponentImpl(
		override final val owner: Owned,
		override val native: NativeComponent = owner.inject(NativeComponent.FACTORY_KEY)(owner)
) : LifecycleBase(), UiComponent {

	override final val injector: Injector = owner.injector

	/**
	 * If true, the native component will be auto-sized to the measured bounds from updateLayout.
	 */
	protected var nativeAutoSize: Boolean = true

	protected val window = inject(Window)
	protected val mouse = inject(MouseState)
	protected val assets = inject(AssetManager)
	protected val interactivity = inject(InteractivityManager)
	protected val timeDriver = inject(TimeDriver)

	private val defaultCamera = inject(Camera)

	// Validatable Properties
	private val _invalidated = own(Signal2<UiComponent, Int>())
	override final val invalidated: Signal<(UiComponent, Int) -> Unit>
		get() = _invalidated

	/**
	 * The root of the validation tree. This is a tree representing how validation flags are resolved.
	 * This may be manipulated, but only on construction.
	 */
	protected var validation: ValidationTree
	private var _isValidating: Boolean = false

	// Transformable properties
	protected val _transform: Matrix4 = Matrix4()
	protected val _concatenatedTransform: Matrix4 = Matrix4()
	protected val _concatenatedTransformInv: Matrix4 = Matrix4()
	protected var _concatenatedTransformInvIsValid: Boolean = false
	protected val _position: Vector3 = Vector3(0f, 0f, 0f)
	protected val _rotation = Vector3(0f, 0f, 0f)
	protected val _scale: Vector3 = Vector3(1f, 1f, 1f)
	protected val _origin: Vector3 = Vector3(0f, 0f, 0f)
	override var cameraOverride: Camera? = null

	/**
	 * True if no scaling or rotation has been applied.
	 */
	protected var isSimpleTranslate: Boolean = true

	// LayoutElement properties
	protected var _bounds = Bounds()
	protected var _explicitWidth: Float? = null
	protected var _explicitHeight: Float? = null
	protected var _layoutData: LayoutData? = null
	protected var _includeInLayout: Boolean = true
	protected val _explicitSizeConstraints: SizeConstraints = SizeConstraints()
	protected val _sizeConstraints: SizeConstraints = SizeConstraints()

	// InteractiveElement properties
	protected var _inheritedInteractivityMode = InteractivityMode.ALL
	override final val inheritedInteractivityMode: InteractivityMode
		get() {
			validate(ValidationFlags.INTERACTIVITY_MODE)
			return _inheritedInteractivityMode
		}

	protected var _interactivityMode: InteractivityMode = InteractivityMode.ALL
	override final var interactivityMode: InteractivityMode
		get() = _interactivityMode
		set(value) {
			if (value != _interactivityMode) {
				_interactivityMode = value
				invalidate(ValidationFlags.INTERACTIVITY_MODE)
			}
		}

	protected val _captureSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	protected val _bubbleSignals = HashMap<InteractionType<*>, StoppableSignal<*>>()
	protected val _attachments = HashMap<Any, Any>()

	// ColorTransformable properties
	protected val _colorTint: Color = Color.WHITE.copy()
	protected val _concatenatedColorTint: Color = Color.WHITE.copy()

	// UiComponent properties
	protected var _visible: Boolean = true

	// Child properties
	override var parent: Container? = null

	private val ownerDisposed = {
		_: Disposable ->
		dispose()
	}

	init {
		owner.disposed.add(ownerDisposed)
		@Suppress("LeakingThis")
		val r = this
		validation = validationTree {
			ValidationFlags.apply {
				addNode(STYLES, r::updateStyles)
				addNode(PROPERTIES, STYLES, r::updateProperties)
				addNode(SIZE_CONSTRAINTS, PROPERTIES, r::validateSizeConstraints)
				addNode(LAYOUT, PROPERTIES or SIZE_CONSTRAINTS, r::validateLayout)
				addNode(TRANSFORM, r::updateTransform)
				addNode(CONCATENATED_TRANSFORM, TRANSFORM, r::updateConcatenatedTransform)
				addNode(COLOR_TRANSFORM, r::updateColorTransform)
				addNode(CONCATENATED_COLOR_TRANSFORM, COLOR_TRANSFORM, r::updateConcatenatedColorTransform)
				addNode(INTERACTIVITY_MODE, r::updateInheritedInteractivityMode)
				addNode(HIERARCHY_ASCENDING, PROPERTIES, r::updateHierarchyAscending)
				addNode(HIERARCHY_DESCENDING, PROPERTIES, r::updateHierarchyDescending)
			}
		}
	}

	//-----------------------------------------------
	// UiComponent
	//-----------------------------------------------

	override final var visible: Boolean
		get() {
			return _visible
		}
		set(value) {
			if (_visible == value) return
			_visible = value
			native.visible = value
			if (value) {
				childWalkLevelOrder {
					// This is only necessary because browsers cannot measure elements with `display: none`.
					// Otherwise we would just invalidate this component's HIERARCHY_ASCENDING.
					it.onAncestorVisibleChanged(this, value)
					TreeWalk.CONTINUE
				}
			}
			invalidate(ValidationFlags.HIERARCHY_ASCENDING)
		}

	override fun onAncestorVisibleChanged(uiComponent: UiComponent, value: Boolean) {
	}

	//-----------------------------------------------
	// LayoutElement
	//-----------------------------------------------

	override fun containsCanvasPoint(canvasX: Float, canvasY: Float): Boolean {
		if (!isActive) return false
		val ray = Ray.obtain()
		camera.getPickRay(canvasX, canvasY, ray)
		val b = intersectsGlobalRay(ray)
		ray.free()
		return b
	}

	/**
	 * The actual bounds of this component.
	 */
	override val bounds: BoundsRo
		get() {
			validate(ValidationFlags.LAYOUT)
			return _bounds
		}

	/**
	 * The explicit width, as set by width(value)
	 * Typically one would use width() in order to retrieve the explicit or actual width.
	 */
	override val explicitWidth: Float?
		get() {
			return _explicitWidth
		}

	/**
	 * The explicit height, as set by height(value)
	 * Typically one would use height() in order to retrieve the explicit or actual height.
	 */
	override val explicitHeight: Float?
		get() {
			return _explicitHeight
		}

	/**
	 * Sets the explicit width. Set to null to use actual width.
	 */
	override fun width(value: Float?) {
		if (_explicitWidth == value) return
		if (value?.isNaN() == true) throw Exception("May not set the size to be NaN")
		_explicitWidth = value
		invalidate(ValidationFlags.LAYOUT)
	}

	override fun height(value: Float?) {
		if (_explicitHeight == value) return
		if (value?.isNaN() == true) throw Exception("May not set the size to be NaN")
		_explicitHeight = value
		invalidate(ValidationFlags.LAYOUT)
	}

	override val shouldLayout: Boolean
		get() {
			return _includeInLayout && _visible
		}

	override final var includeInLayout: Boolean
		get() {
			return _includeInLayout
		}
		set(value) {
			if (_includeInLayout == value) return
			_includeInLayout = value
			invalidate(ValidationFlags.HIERARCHY_ASCENDING)
		}


	protected val layoutDataChangedHandler = {
		invalidate(ValidationFlags.LAYOUT)
		Unit
	}

	override var layoutData: LayoutData?
		get() {
			return _layoutData
		}
		set(value) {
			if (_layoutData == value) return
			if (_layoutData != null) {
				_layoutData!!.changed.remove(layoutDataChangedHandler)
			}
			_layoutData = value
			if (_layoutData != null) {
				_layoutData!!.changed.add(layoutDataChangedHandler)
			}
			invalidate(ValidationFlags.LAYOUT)
		}

	/**
	 * Returns the measured size constraints, bound by the explicit size constraints.
	 */
	override val sizeConstraints: SizeConstraintsRo
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints
		}

	/**
	 * Returns the explicit size constraints.
	 */
	override val explicitSizeConstraints: SizeConstraintsRo
		get() {
			return _explicitSizeConstraints
		}

	override val minWidth: Float?
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints.width.min
		}

	override fun minWidth(value: Float?) {
		_explicitSizeConstraints.width.min = value
		invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	}

	override val minHeight: Float?
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints.height.min
		}

	override fun minHeight(value: Float?) {
		_explicitSizeConstraints.height.min = value
		invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	}

	override val maxWidth: Float?
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints.width.max
		}

	override fun maxWidth(value: Float?) {
		_explicitSizeConstraints.width.max = value
		invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	}

	override val maxHeight: Float?
		get() {
			validate(ValidationFlags.SIZE_CONSTRAINTS)
			return _sizeConstraints.height.max
		}

	override fun maxHeight(value: Float?) {
		_explicitSizeConstraints.height.max = value
		invalidate(ValidationFlags.SIZE_CONSTRAINTS)
	}

	/**
	 * Does the same thing as setting width and height individually.
	 */
	override fun setSize(width: Float?, height: Float?) {
		if (width?.isNaN() == true || height?.isNaN() == true) throw Exception("May not set the size to be NaN")
		if (_explicitWidth == width && _explicitHeight == height) return
		_explicitWidth = width
		_explicitHeight = height
		invalidate(ValidationFlags.LAYOUT)
	}

	/**
	 * Do not call this directly, use [validate(ValidationFlags.SIZE_CONSTRAINTS)]
	 */
	protected fun validateSizeConstraints() {
		_sizeConstraints.clear()
		updateSizeConstraints(_sizeConstraints)
		_sizeConstraints.bound(_explicitSizeConstraints)
	}

	/**
	 * Updates the measured size constraints object.
	 */
	protected open fun updateSizeConstraints(out: SizeConstraints) {
	}

	/**
	 * Do not call this directly, use [validate(ValidationFlags.LAYOUT)]
	 */
	protected fun validateLayout() {
		val sC = sizeConstraints
		val w = sC.width.clamp(_explicitWidth)
		val h = sC.height.clamp(_explicitHeight)
		_bounds.set(w ?: 0f, h ?: 0f)
		updateLayout(w, h, _bounds)
		if (assertionsEnabled && (_bounds.width.isNaN() || _bounds.height.isNaN()))
			throw Exception("Bounding measurements should not be NaN")

		if (nativeAutoSize)
			native.setSize(_bounds.width, _bounds.height)
	}

	/**
	 * Updates this LayoutElement's layout.
	 * This method should update the [out] [Rectangle] bounding measurements.
	 *
	 * @param explicitWidth The explicitWidth dimension. Null if the preferred width should be used.
	 * @param explicitHeight The explicitHeight dimension. Null if the preferred height should be used.
	 */
	protected open fun updateLayout(explicitWidth: Float?, explicitHeight: Float?, out: Bounds) {
	}

	//-----------------------------------------------
	// InteractiveElement
	//-----------------------------------------------


	override fun hasInteraction(): Boolean {
		return _captureSignals.isNotEmpty() || _bubbleSignals.isNotEmpty()
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : InteractionEvent> getInteractionSignal(type: InteractionType<T>, isCapture: Boolean): StoppableSignal<T>? {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		return handlers[type] as StoppableSignal<T>?
	}

	override fun <T : InteractionEvent> addInteractionSignal(type: InteractionType<T>, signal: StoppableSignal<T>, isCapture: Boolean) {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		handlers[type] = signal
	}

	override fun <T : InteractionEvent> removeInteractionSignal(type: InteractionType<T>, isCapture: Boolean) {
		val handlers = if (isCapture) _captureSignals else _bubbleSignals
		handlers.remove(type)
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> getAttachment(key: Any): T? {
		return _attachments[key] as T?
	}

	override fun setAttachment(key: Any, value: Any) {
		_attachments.put(key, value)
	}

	/**
	 * Removes an attachment added via [setAttachment]
	 */
	@Suppress("UNCHECKED_CAST")
	override fun <T : Any> removeAttachment(key: Any): T? {
		return _attachments.remove(key) as T?
	}

	/**
	 * Sets the [out] vector to the local mouse coordinates.
	 * @return Returns the [out] vector.
	 */
	override fun mousePosition(out: Vector2): Vector2 {
		windowToLocal(mouse.mousePosition(out))
		return out
	}

	override fun mouseIsOver(): Boolean {
		if (!isActive || !mouse.overCanvas()) return false
		val stage = owner.injectOptional(Stage) ?: return false
		val e = stage.getChildUnderPoint(mouse.canvasX(), mouse.canvasY(), onlyInteractive = true) ?: return false
		return e.isDescendantOf(this)
	}

	//-----------------------------------------------
	// ColorTransformable
	//-----------------------------------------------

	/**
	 * The color tint of this component.
	 * The final pixel color value for the default shader is [colorTint * pixel]
	 *
	 * If this is modified directly, be sure to call [invalidate(ValidationFlags.COLOR_TRANSFORM)]
	 */
	override var colorTint: ColorRo
		get() {
			return _colorTint
		}
		set(value) {
			if (_colorTint == value) return
			_colorTint.set(value)
			invalidate(ValidationFlags.COLOR_TRANSFORM)
		}

	override fun colorTint(r: Float, g: Float, b: Float, a: Float) {
		_colorTint.set(r, g, b, a)
		invalidate(ValidationFlags.COLOR_TRANSFORM)
	}

	/**
	 * The color multiplier of this component and all ancestor color tints multiplied together.
	 * Do not set this directly, it will be overwritten on a [ValidationFlags.CONCATENATED_COLOR_TRANSFORM] validation.
	 * Retrieving this value validates [ValidationFlags.CONCATENATED_COLOR_TRANSFORM]
	 */
	override val concatenatedColorTint: ColorRo
		get() {
			validate(ValidationFlags.CONCATENATED_COLOR_TRANSFORM)
			return _concatenatedColorTint
		}

	/**
	 * Concatenates the color transform.
	 * Do not call this directly, use `validate(ValidationFlags.CONCATENATED_COLOR_TRANSFORM)`
	 */
	protected open fun updateColorTransform() {
		native.setColorTint(_colorTint)
	}

	protected open fun updateConcatenatedColorTransform() {
		val p = parent
		if (p == null) {
			_concatenatedColorTint.set(_colorTint)
		} else {
			_concatenatedColorTint.set(p.concatenatedColorTint).mul(_colorTint)
		}
		native.setConcatenatedColorTint(_concatenatedColorTint)
	}

	protected open fun updateInheritedInteractivityMode() {
		_inheritedInteractivityMode = _interactivityMode
		if (parent?.inheritedInteractivityMode == InteractivityMode.NONE)
			_inheritedInteractivityMode = InteractivityMode.NONE

		native.interactivityEnabled = _inheritedInteractivityMode == InteractivityMode.ALL
	}

	protected open fun updateHierarchyAscending() {}
	protected open fun updateHierarchyDescending() {}

	//-----------------------------------------------
	// Interactivity utility methods
	//-----------------------------------------------


	override fun getChildrenUnderRay(globalRay: Ray, out: ArrayList<UiComponent>, onlyInteractive: Boolean, returnAll: Boolean) {
		if (!_visible || (onlyInteractive && !interactivityEnabled)) return
		if (intersectsGlobalRay(globalRay)) {
			out.add(this)
		}
	}

	//-----------------------------------------------
	// Styleable
	//-----------------------------------------------

	override val styleParent: Styleable? by lazy {
		var p: Owned? = owner
		var s: Styleable? = null
		while (p != null) {
			if (p is Styleable) {
				s = p
				break
			}
			p = p.owner
		}
		s
	}

	private var _styles: StylesImpl? = null
	private val styles: StylesImpl
		get() {
			if (_styles == null) _styles = own(StylesImpl(this))
			return _styles!!
		}

	override final val styleTags: MutableList<StyleTag>
		get() = styles.styleTags

	override final val styleRules: MutableList<StyleRule<*>>
		get() = styles.styleRules

	override fun <T : Style> getRulesByType(type: StyleType<T>): List<StyleRule<T>>? = styles.getRulesByType(type)

	protected fun <T : MutableStyle> bind(style: T, calculator: StyleCalculator = CascadingStyleCalculator): T {
		styles.bind(style, calculator)
		return style
	}
	protected fun <T : MutableStyle> watch(style: T, priority: Float = 0f, callback: (T) -> Unit) = styles.watch(style, priority, callback)
	protected fun unwatch(style: MutableStyle) = styles.unwatch(style)

	protected fun unbind(style: Style) = styles.unbind(style)

	protected open fun updateStyles() {
		_styles?.validateStyles()
	}

	//-----------------------------------------------
	// Validatable properties
	//-----------------------------------------------


	/**
	 * Validates this component's properties. This is done before any layout or transformation
	 * validation happens.
	 * Do not call this directly, use `validate(ValidationFlags.PROPERTIES)`
	 */
	protected open fun updateProperties() {
	}

	//-----------------------------------------------
	// Transformable
	//-----------------------------------------------

	/**
	 * This component's transformation matrix.
	 * Responsible for positioning, scaling, rotation, etc.
	 *
	 * Do not modify this matrix directly, but instead use the exposed transformation properties:
	 * x, y, scaleX, scaleY, rotation
	 */
	override val transform: Matrix4Ro
		get() {
			validate(ValidationFlags.TRANSFORM)
			return _transform
		}

	private var _customTransform: Matrix4Ro? = null
	override var customTransform: Matrix4Ro?
		get() = _customTransform
		set(value) {
			isSimpleTranslate = false
			_customTransform = value
			invalidate(ValidationFlags.TRANSFORM)
		}


	override var rotationX: Float
		get() = _rotation.x
		set(value) {
			if (_rotation.x == value) return
			_rotation.x = value
			if (isSimpleTranslate && value != 0f) {
				isSimpleTranslate = false
			}
			invalidate(ValidationFlags.TRANSFORM)
			return
		}

	override var rotationY: Float
		get() = _rotation.y
		set(value) {
			if (_rotation.y == value) return
			_rotation.y = value
			if (isSimpleTranslate && value != 0f) {
				isSimpleTranslate = false
			}
			invalidate(ValidationFlags.TRANSFORM)
			return
		}

	/**
	 * Rotation around the Z axis
	 */
	override var rotation: Float
		get() = _rotation.z
		set(value) {
			if (_rotation.z == value) return
			_rotation.z = value
			if (isSimpleTranslate && value != 0f) {
				isSimpleTranslate = false
			}
			invalidate(ValidationFlags.TRANSFORM)
		}

	override fun setRotation(x: Float, y: Float, z: Float) {
		if (_rotation.x == x && _rotation.y == y && _rotation.z == z) return
		_rotation.set(x, y, z)
		if (isSimpleTranslate && (x != 0f || y != 0f || z != 0f)) {
			isSimpleTranslate = false
		}
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	//-----------------------------------------------
	// Transformation and translation methods
	//-----------------------------------------------

	override var x: Float
		get() = _position.x
		set(value) {
			if (value == _position.x) return
			_position.x = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var y: Float
		get() = _position.y
		set(value) {
			if (value == _position.y) return
			_position.y = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var z: Float
		get() = _position.z
		set(value) {
			if (value == _position.z) return
			_position.z = value
			if (isSimpleTranslate && value != 0f)
				isSimpleTranslate = false
			invalidate(ValidationFlags.TRANSFORM)
		}

	override val position: Vector3Ro
		get() = _position

	/**
	 * Does the same thing as setting width and height individually.
	 */
	override fun setPosition(x: Float, y: Float, z: Float) {
		if (x == _position.x && y == _position.y && z == _position.z) return
		_position.set(x, y, z)
		if (isSimpleTranslate && z != 0f)
			isSimpleTranslate = false
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	override var scaleX: Float
		get() = _scale.x
		set(value) {
			if (_scale.x == value) return
			_scale.x = value
			if (isSimpleTranslate && value != 1f)
				isSimpleTranslate = false
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var scaleY: Float
		get() = _scale.y
		set(value) {
			if (_scale.y == value) return
			_scale.y = value
			if (isSimpleTranslate && value != 1f)
				isSimpleTranslate = false
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var scaleZ: Float
		get() = _scale.z
		set(value) {
			if (_scale.z == value) return
			_scale.z = value
			if (isSimpleTranslate && value != 1f)
				isSimpleTranslate = false
			invalidate(ValidationFlags.TRANSFORM)
		}

	override fun setScaling(x: Float, y: Float, z: Float) {
		if (_scale.x == x && _scale.y == y && _scale.z == z) return
		_scale.set(x, y, z)
		if (isSimpleTranslate && (x != 1f || y != 1f || z != 1f))
			isSimpleTranslate = false
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	override var originX: Float
		get() = _origin.x
		set(value) {
			if (_origin.x == value) return
			_origin.x = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var originY: Float
		get() = _origin.y
		set(value) {
			if (_origin.y == value) return
			_origin.y = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override var originZ: Float
		get() = _origin.z
		set(value) {
			if (_origin.z == value) return
			_origin.z = value
			invalidate(ValidationFlags.TRANSFORM)
		}

	override fun setOrigin(x: Float, y: Float, z: Float) {
		if (_origin.x == x && _origin.y == y && _origin.z == z) return
		_origin.set(x, y, z)
		invalidate(ValidationFlags.TRANSFORM)
		return
	}

	/**
	 * The global transform of this component, of all ancestor transforms multiplied together.
	 * Do not modify this matrix directly, it will be overwritten on a TRANSFORM validation.
	 */
	override val concatenatedTransform: Matrix4Ro
		get() {
			validate(ValidationFlags.CONCATENATED_TRANSFORM)
			return _concatenatedTransform
		}

	/**
	 * Returns the inverse concatenated transformation matrix.
	 */
	override val concatenatedTransformInv: Matrix4Ro
		get() {
			validate(ValidationFlags.CONCATENATED_TRANSFORM)
			if (!_concatenatedTransformInvIsValid) {
				_concatenatedTransformInvIsValid = true
				_concatenatedTransformInv.set(_concatenatedTransform)
				_concatenatedTransformInv.inv()
			}
			return _concatenatedTransformInv
		}

	/**
	 * Applies all operations to the transformation matrix.
	 * Do not call this directly, use [validate(ValidationFlags.TRANSFORM)]
	 */
	protected open fun updateTransform() {
		if (_customTransform != null) {
			_transform.set(_customTransform!!)
			return
		}
		_transform.idt()
		if (isSimpleTranslate) {
			_transform.trn(_position.x - _origin.x, _position.y - _origin.y, 0f)
			native.setSimpleTranslate(_position.x - _origin.x, _position.y - _origin.y)
		} else {
			_transform.trn(_position)
			_transform.scl(_scale)
			if (!_rotation.isZero()) {
				quat.setEulerAnglesRad(_rotation.y, _rotation.x, _rotation.z)
				_transform.rotate(quat)
			}
			if (!_origin.isZero())
				_transform.translate(-_origin.x, -_origin.y, -_origin.z)
			native.setTransform(_transform)
		}
	}

	/**
	 * Updates this component's concatenatedTransform matrix, which is the parent's concatenatedTransform
	 * multiplied by this component's transform matrix.
	 *
	 * Do not call this directly, use [validate(ValidationFlags.CONCATENATED_TRANSFORM)]
	 */
	protected open fun updateConcatenatedTransform() {
		val p = parent
		if (p != null) {
			if (isSimpleTranslate) {
				_concatenatedTransform.set(p.concatenatedTransform).translate(_position.x - _origin.x, _position.y - _origin.y, _position.z - _origin.z)
			} else {
				_concatenatedTransform.set(p.concatenatedTransform).mul(_transform)
			}
		} else {
			_concatenatedTransform.set(_transform)
		}
		_concatenatedTransformInvIsValid = false
		native.setConcatenatedTransform(_concatenatedTransform)
	}

	/**
	 * Returns the camera to be used for this component.
	 */
	override val camera: Camera
		get() {
			if (cameraOverride != null) return cameraOverride!!
			val p = parent
			if (p != null) {
				return p.camera
			} else {
				return defaultCamera
			}
		}

	//-----------------------------------------------
	// Validatable
	//-----------------------------------------------

	override fun invalidate(flags: Int): Int {
		val flagsInvalidated: Int = validation.invalidate(flags)

		if (flagsInvalidated != 0) {
			window.requestRender()
			onInvalidated(flagsInvalidated)
			_invalidated.dispatch(this, flagsInvalidated)
		}
		return flagsInvalidated
	}

	protected open fun onInvalidated(flagsInvalidated: Int) {
	}

	/**
	 * Validates the specified flags for this component.
	 *
	 * @param flags A bit mask for which flags to validate. (Use -1 to validate all)
	 * Example: validate(ValidationFlags.LAYOUT or ValidationFlags.PROPERTIES) to validate both layout an properties.
	 * @param force If true, the provided flags will be validated, even if they are not currently invalid.
	 */
	override fun validate(flags: Int, force: Boolean) {
		if (_isValidating) return
		_isValidating = true
		validation.validate(flags, force)
		_isValidating = false
	}

	//-----------------------------------------------
	// Drivable
	//-----------------------------------------------

	override fun update() {
		validate()
	}

	//-----------------------------------------------
	// Renderable
	//-----------------------------------------------

	/**
	 * Responsible for rendering this component, including any applied filters.
	 * Typically, custom components override the [draw] method.
	 */
	override fun render() {
		if (!visible || _concatenatedColorTint.a <= 0f) return // Nothing visible.
		draw()
	}

	/**
	 * The core drawing method for this component.
	 */
	protected open fun draw() {
	}

	//-----------------------------------------------
	// Disposable
	//-----------------------------------------------

	override fun dispose() {
		super.dispose()
		owner.disposed.remove(ownerDisposed)
		if (assertionsEnabled) {
			parentWalk {
				if (!owner.owns(it)) {
					throw Exception("this component must be removed before disposing.")
				}
				true
			}
		}

		layoutData?.dispose()
		layoutData = null

		// InteractiveElement
		// Dispose all disposable handlers.
		for (i in _captureSignals.values) {
			(i as? Disposable)?.dispose()
		}
		_captureSignals.clear()
		for (i in _bubbleSignals.values) {
			(i as? Disposable)?.dispose()
		}
		_bubbleSignals.clear()
		// Dispose all disposable attachments.
		for (i in _attachments.values) {
			(i as? Disposable)?.dispose()
		}
		_attachments.clear()
		native.dispose()
	}

	companion object {
		private val quat: Quaternion = Quaternion()
	}
}

interface NativeComponent : Disposable {

	var interactivityEnabled: Boolean

	/**
	 * Sets the visibility of this component.
	 */
	var visible: Boolean

	/**
	 * Return the actual bounds of the component.
	 * This is typically either the explicit dimensions, if they were set, or the measured dimensions.
	 */
	val bounds: Bounds

	/**
	 * Sets the explicit dimensions of this component. Null values represent using measured dimensions.
	 */
	fun setSize(width: Float?, height: Float?)

	/**
	 * Sets the transformation matrix.
	 * Do not use both this method and [setSimpleTranslate]
	 */
	fun setTransform(value: Matrix4)

	/**
	 * An alternate to [setTransform] that applies only a simple translation.
	 * Do not use both this method and [setTransform]
	 */
	fun setSimpleTranslate(x: Float, y: Float)

	/**
	 * Sets the concatenated global transformation matrix.
	 */
	fun setConcatenatedTransform(value: Matrix4)

	/**
	 * Sets the color tint.
	 */
	fun setColorTint(value: Color)

	/**
	 * Sets the combined global color tint.
	 */
	fun setConcatenatedColorTint(value: Color)

	companion object {
		val FACTORY_KEY: DKey<(owner: Owned) -> NativeComponent> = DependencyKeyImpl()
	}
}

object NativeComponentDummy : NativeComponent {

	override var interactivityEnabled: Boolean = true

	override var visible: Boolean = true

	override val bounds: Bounds
		get() = throw UnsupportedOperationException("NativeComponentDummy does not have bounds.")

	override fun setSize(width: Float?, height: Float?) {
	}

	override fun setTransform(value: Matrix4) {
	}

	override fun setSimpleTranslate(x: Float, y: Float) {
	}

	override fun setConcatenatedTransform(value: Matrix4) {
	}

	override fun setColorTint(value: Color) {
	}

	override fun setConcatenatedColorTint(value: Color) {
	}

	override fun dispose() {
	}
}