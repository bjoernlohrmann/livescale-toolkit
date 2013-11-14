package de.tuberlin.cit.livestream.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class StatusOverlayView extends View {
	private String message;

	public StatusOverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.message = null;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Paint paint = new Paint();
		paint.setStrokeWidth(1);
		paint.setTextSize(14);
		paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
		paint.setColor(Color.WHITE);

		if (message != null) {
			canvas.drawText(message, 10, 20, paint);
		}
	}
}