package rega.genotype.ui.framework.widgets;

import eu.webtoolkit.jwt.AbstractSignal;
import eu.webtoolkit.jwt.Cursor;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.TextFormat;
import eu.webtoolkit.jwt.WCompositeWidget;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFormWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WTextArea;

public class InPlaceEdit extends WCompositeWidget {

	private Signal1<WString> valueChanged_;
	private WContainerWidget impl_;
	private WContainerWidget editing_;
	private WText text_;
	private WString emptyText_;
	private WFormWidget edit_;
	private WPushButton save_;
	private WPushButton cancel_;
	private AbstractSignal.Connection c1_;
	private AbstractSignal.Connection c2_;
	private boolean empty_;

	public InPlaceEdit() {
		this("", null, null);
	}

	public InPlaceEdit(CharSequence text) {
		this(text, null, null);
	}
	
	/**
	 * A widget that can have 2 modes - show text or edit.
	 * @param text init text
	 * @param edit a widget that will be used for editing.
	 * @param parent
	 */
	public InPlaceEdit(CharSequence text, WFormWidget edit, WContainerWidget parent) {
		super(parent);
		this.valueChanged_ = new Signal1<WString>(this);
		this.emptyText_ = new WString("(Empty)");
//		this.c1_ = new AbstractSignal.Connection();
//		this.c2_ = new AbstractSignal.Connection();
		this.setImplementation(this.impl_ = new WContainerWidget());
		this.getDecorationStyle().setCursor(Cursor.ArrowCursor);

		this.text_ = new WText(WString.Empty, TextFormat.XHTMLText, this.impl_);
		this.text_.getDecorationStyle().setCursor(Cursor.PointingHandCursor);
		this.editing_ = new WContainerWidget(this.impl_);
		this.editing_.setInline(true);
		this.editing_.hide();
		this.editing_.addStyleClass("input-append");
		
		setInline(false);

		if (edit == null){
			edit_ = new WTextArea(editing_);
			((WTextArea)edit_).setRows(1);
			edit_.setWidth(new WLength(150));
		} else {
			edit_ = edit;
			editing_.addWidget(edit_);
		}
		
		this.save_ = null;
		this.cancel_ = null;
		this.text_.clicked().addListener(this.text_,
				new Signal1.Listener<WMouseEvent>() {
					public void trigger(WMouseEvent e1) {
						InPlaceEdit.this.text_.hide();
					}
				});
		this.text_.clicked().addListener(this.text_,
				new Signal1.Listener<WMouseEvent>() {
					public void trigger(WMouseEvent e1) {
						InPlaceEdit.this.editing_.show();
					}
				});
		this.text_.clicked().addListener(this.text_,
				new Signal1.Listener<WMouseEvent>() {
					public void trigger(WMouseEvent e1) {
						InPlaceEdit.this.edit_.setFocus();
					}
				});
		
		this.edit_.escapePressed().addListener(this.editing_,
				new Signal.Listener() {
					public void trigger() {
						InPlaceEdit.this.editing_.hide();
					}
				});
		this.edit_.escapePressed().addListener(this.text_,
				new Signal.Listener() {
					public void trigger() {
						InPlaceEdit.this.text_.show();
					}
				});
		this.edit_.escapePressed().addListener(this, new Signal.Listener() {
			public void trigger() {
				InPlaceEdit.this.cancel();
			}
		});
		this.edit_.escapePressed().preventPropagation();
		this.setButtonsEnabled();
		this.setText(text);
	}

	public void setInTableRowStyle(boolean inline){
		edit_.setInline(inline);
		editing_.setInline(inline);
		impl_.setInline(inline);
		this.editing_.addStyleClass("inplace-edit");
		setInline(inline);
		edit_.addStyleClass("inplace-edit");
	}

	public WString getText() {
		return new WString(this.edit_.getValueText());
	}

	public void setText(CharSequence text) {
		if (!(text.length() == 0)) {
			this.text_.setText(text);
			this.empty_ = false;
		} else {
			this.text_.setText(this.emptyText_);
			this.empty_ = true;
		}
		this.edit_.setValueText(text.toString());
	}

	public void setEmptyText(CharSequence emptyText) {
		this.emptyText_ = WString.toWString(emptyText);
		if (this.empty_) {
			this.text_.setText(this.emptyText_);
		}
	}

	public WString getEmptyText() {
		return this.emptyText_;
	}

	public WFormWidget getEdit() {
		return this.edit_;
	}

	public WText getTextWidget() {
		return this.text_;
	}

	public WPushButton getSaveButton() {
		return this.save_;
	}

	public WPushButton getCancelButton() {
		return this.cancel_;
	}

	public Signal1<WString> valueChanged() {
		return this.valueChanged_;
	}

	public void setButtonsEnabled(boolean enabled) {
		if (c1_ != null && this.c1_.isConnected()) {
			this.c1_.disconnect();
		}
		if (c2_ != null && this.c2_.isConnected()) {
			this.c2_.disconnect();
		}
		if (enabled) {
			this.save_ = new WPushButton(tr("Wt.WInPlaceEdit.Save"),
					this.editing_);
			this.cancel_ = new WPushButton(tr("Wt.WInPlaceEdit.Cancel"),
					this.editing_);
			this.save_.clicked().addListener(this.edit_,
					new Signal1.Listener<WMouseEvent>() {
						public void trigger(WMouseEvent e1) {
							InPlaceEdit.this.edit_.disable();
						}
					});
			this.save_.clicked().addListener(this.save_,
					new Signal1.Listener<WMouseEvent>() {
						public void trigger(WMouseEvent e1) {
							InPlaceEdit.this.save_.disable();
						}
					});
			this.save_.clicked().addListener(this.cancel_,
					new Signal1.Listener<WMouseEvent>() {
						public void trigger(WMouseEvent e1) {
							InPlaceEdit.this.cancel_.disable();
						}
					});
			this.save_.clicked().addListener(this,
					new Signal1.Listener<WMouseEvent>() {
						public void trigger(WMouseEvent e1) {
							InPlaceEdit.this.save();
						}
					});
			this.cancel_.clicked().addListener(this.editing_,
					new Signal1.Listener<WMouseEvent>() {
						public void trigger(WMouseEvent e1) {
							InPlaceEdit.this.editing_.hide();
						}
					});
			this.cancel_.clicked().addListener(this.text_,
					new Signal1.Listener<WMouseEvent>() {
						public void trigger(WMouseEvent e1) {
							InPlaceEdit.this.text_.show();
						}
					});
			this.cancel_.clicked().addListener(this,
					new Signal1.Listener<WMouseEvent>() {
						public void trigger(WMouseEvent e1) {
							InPlaceEdit.this.cancel();
						}
					});
		} else {
			if (this.save_ != null)
				this.save_.remove();
			this.save_ = null;
			if (this.cancel_ != null)
				this.cancel_.remove();
			this.cancel_ = null;

			this.c1_ = this.edit_.blurred().addListener(this.edit_,
					new Signal.Listener() {
						public void trigger() {
							InPlaceEdit.this.edit_.disable();
						}
					});
			this.c2_ = this.edit_.blurred().addListener(this,
	 				new Signal.Listener() {
						public void trigger() {
							InPlaceEdit.this.save();
						}
					});
		}
	}

	/**
	 * Displays the Save and &apos;Cancel&apos; button during editing.
	 * <p>
	 * Calls {@link #setButtonsEnabled(boolean enabled) setButtonsEnabled(true)}
	 */
	public final void setButtonsEnabled() {
		setButtonsEnabled(true);
	}

	private void save() {
		this.editing_.hide();
		this.text_.show();
		this.edit_.enable();
		if (this.save_ != null) {
			this.save_.enable();
		}
		if (this.cancel_ != null) {
			this.cancel_.enable();
		}
		boolean changed = this.empty_ ? this.edit_.getValueText().length() != 0
				: !this.edit_.getValueText().equals(this.text_.getText().toString());
		if (changed) {
			this.setText(this.edit_.getValueText());
			this.valueChanged().trigger(new WString(this.edit_.getValueText()));
		}
	}

	private void cancel() {
		this.edit_.setValueText((this.empty_ ? WString.Empty : this.text_.getText())
				.toString());
	}
}