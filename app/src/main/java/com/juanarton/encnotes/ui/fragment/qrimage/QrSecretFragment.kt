package com.juanarton.encnotes.ui.fragment.qrimage

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.juanarton.encnotes.R
import com.juanarton.encnotes.databinding.FragmentQrSecretBinding

class QrSecretFragment(
    private val qrImage: String,
    private val secret: String
) : Fragment() {

    private var _binding: FragmentQrSecretBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentQrSecretBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val base64Image = qrImage.substringAfter("base64,")
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)

        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        binding?.apply {
            Glide.with(requireContext())
                .load(bitmap)
                .into(ivQrSecret)

            tvTwoFaSecret.text = secret

            btDone.setOnClickListener {
                destroySelf()
            }
        }
    }

    fun destroySelf() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()
    }
}