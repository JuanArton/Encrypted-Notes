package com.juanarton.privynote.ui.fragment.insertkey

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.juanarton.privynote.R
import com.juanarton.privynote.databinding.FragmentInsertKeyBinding
import com.juanarton.privynote.ui.activity.main.MainActivity
import com.juanarton.privynote.ui.fragment.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InsertKeyFragment(
) : Fragment() {

    private val sharedViewModel: SharedViewModel by viewModels()
    private var _binding: FragmentInsertKeyBinding? = null
    private val binding get() = _binding
    private lateinit var key: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentInsertKeyBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            btDone.setOnClickListener {
                key = etCipherKey.text.toString()
                if (key.isNotEmpty()) {
                    lifecycleScope.launch {
                        val setCipherKey = if (::key.isInitialized){
                            sharedViewModel.setCipherKey(key)
                        } else { false }

                        if (setCipherKey) {
                            startActivity(Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(), getString(R.string.please_insert_cipher_key
                    ), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}